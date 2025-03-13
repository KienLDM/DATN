package com.example.kiendatn2

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val usersCollection = firestore.collection("users")
    private val postsCollection = firestore.collection("posts")
    private val commentsCollection = firestore.collection("comments")
    private val likesCollection = firestore.collection("likes")

    // Current user operations
    suspend fun createUserProfile(user: User) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
        val userData = user.copy(id = currentUser.uid, email = currentUser.email ?: "")
        usersCollection.document(currentUser.uid).set(userData).await()
    }

    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext null
        usersCollection.document(currentUser.uid).get().await().toObject(User::class.java)
    }

    // Post operations
    suspend fun createPost(text: String, imageUri: Uri?): Post = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        // If image is included, upload to storage first
        val imageUrl = imageUri?.let { uploadImage(it, "posts") }

        val postId = postsCollection.document().id
        val post = Post(
            id = postId,
            userId = currentUser.uid,
            text = text,
            imageUrl = imageUrl,
            createdAt = Timestamp.now()
        )

        postsCollection.document(postId).set(post).await()
        post
    }

    suspend fun getPosts(): List<Post> = withContext(Dispatchers.IO) {
        postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Post::class.java)
    }

    // Add function to check if posts are liked by current user
    suspend fun getPostsWithLikeStatus(): List<Post> = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext emptyList<Post>()
        
        // Get all posts
        val posts = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Post::class.java)
        
        // Get all likes by current user
        val userLikes = likesCollection
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .await()
            .documents
            .map { it.getString("postId") ?: "" }
            .toSet()
        
        // Mark posts as liked if they are in the userLikes set
        return@withContext posts.map { post ->
            post.copy(isLikedByCurrentUser = userLikes.contains(post.id))
        }
    }

    // Comment operations
    suspend fun addComment(postId: String, text: String, imageUri: Uri?): Comment = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        // If image is included, upload to storage first
        val imageUrl = imageUri?.let { uploadImage(it, "comments") }

        val commentId = commentsCollection.document().id
        val comment = Comment(
            id = commentId,
            postId = postId,
            userId = currentUser.uid,
            text = text,
            imageUrl = imageUrl,
            createdAt = Timestamp.now()
        )

        commentsCollection.document(commentId).set(comment).await()

        // Increment comment count
        postsCollection.document(postId).update("commentCount",
            com.google.firebase.firestore.FieldValue.increment(1)).await()

        comment
    }

    suspend fun getCommentsForPost(postId: String): List<Comment> = withContext(Dispatchers.IO) {
        commentsCollection
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .await()
            .toObjects(Comment::class.java)
    }

    suspend fun getPostById(postId: String): Post? = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
        
        val post = postsCollection
            .document(postId)
            .get()
            .await()
            .toObject(Post::class.java) ?: return@withContext null
        
        // Check if post is liked by current user
        val isLiked = if (currentUser != null) {
            !likesCollection
                .whereEqualTo("postId", postId)
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                .isEmpty
        } else {
            false
        }
        
        return@withContext post.copy(isLikedByCurrentUser = isLiked)
    }

    // Like operations
    suspend fun toggleLike(postId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        val likeQuery = likesCollection
            .whereEqualTo("postId", postId)
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .await()

        if (likeQuery.isEmpty) {
            // Add like
            val likeId = likesCollection.document().id
            val likeData = hashMapOf(
                "id" to likeId,
                "postId" to postId,
                "userId" to currentUser.uid,
                "createdAt" to Timestamp.now()
            )

            likesCollection.document(likeId).set(likeData).await()
            postsCollection.document(postId).update("likeCount",
                com.google.firebase.firestore.FieldValue.increment(1)).await()
            true
        } else {
            // Remove like
            val likeDoc = likeQuery.documents.first()
            likesCollection.document(likeDoc.id).delete().await()
            postsCollection.document(postId).update("likeCount",
                com.google.firebase.firestore.FieldValue.increment(-1)).await()
            false
        }
    }

    // Helper method for image uploads
    private suspend fun uploadImage(imageUri: Uri, folder: String): String = withContext(Dispatchers.IO) {
        val filename = UUID.randomUUID().toString()
        val ref = storage.reference.child("$folder/$filename")
        ref.putFile(imageUri).await()
        ref.downloadUrl.await().toString()
    }
}