package com.example.kiendatn2.repository

import android.net.Uri
import com.example.kiendatn2.data.Comment
import com.example.kiendatn2.data.Post
import com.example.kiendatn2.data.User
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
    private val commentLikesCollection = firestore.collection("commentLikes")

    // Current user operations
    fun getCurrentFirebaseUser() = auth.currentUser
    
    suspend fun createUserProfile(user: User) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
        val userData = user.copy(id = currentUser.uid, email = currentUser.email ?: "")
        usersCollection.document(currentUser.uid).set(userData).await()
    }

    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext null
        usersCollection.document(currentUser.uid).get().await().toObject(User::class.java)
    }

    // User profile operations
    suspend fun updateUserProfile(displayName: String, bio: String?, photoUri: Uri?): User = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        // Upload photo if provided - handle potential failures
        var photoUrl = getCurrentUser()?.photoUrl
        if (photoUri != null) {
            try {
                photoUrl = uploadImage(photoUri, "profiles")
            } catch (e: Exception) {
                // Log error but continue with profile update without changing photo
                android.util.Log.e(
                    "FirebaseRepository",
                    "Failed to upload profile photo: ${e.message}"
                )
            }
        }
        
        // Create updated user data
        val userData = mapOf(
            "displayName" to displayName,
            "bio" to bio,
            "photoUrl" to photoUrl
        )
        
        // Update user document
        usersCollection.document(currentUser.uid).update(userData).await()
        
        // Return updated user
        getCurrentUser() ?: throw IllegalStateException("Failed to get updated user")
    }

    // Get posts by current user
    suspend fun getUserPosts(): List<Post> = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
        
        try {
            // Get posts by the current user
            val posts = postsCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                .toObjects(Post::class.java)
                .sortedByDescending { it.createdAt }
                
            // Get likes by current user to mark which posts are liked
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
        } catch (e: Exception) {
            // Fallback to a simpler query if index not available
            val posts = postsCollection
                .get()
                .await()
                .toObjects(Post::class.java)
                .filter { it.userId == currentUser.uid }
                .sortedByDescending { it.createdAt }
                
            return@withContext posts
       }
    }

    // Post operations
    suspend fun createPost(text: String, imageUri: Uri?): Post = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
        
        // Get the user display name
        val userDisplayName = currentUser.displayName ?: "Unknown User"

        // If image is included, try to upload to storage first
        var imageUrl: String? = null
        if (imageUri != null) {
            try {
                imageUrl = uploadImage(imageUri, "posts")
            } catch (e: Exception) {
                // Log the error but continue creating post without image
                android.util.Log.e("FirebaseRepository", "Failed to upload image: ${e.message}")
                // If image upload fails, we'll proceed with creating the post without an image
            }
        }

        val postId = postsCollection.document().id
        val post = Post(
            id = postId,
            userId = currentUser.uid,
            userDisplayName = userDisplayName, 
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
        
        try {
            // Get all posts
            val posts = postsCollection
                .get()
                .await()
                .toObjects(Post::class.java)
                .sortedByDescending { it.createdAt }
            
            // Get all likes by current user
            val userLikes = likesCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                .documents
                .map { it.getString("postId") ?: "" }
                .toSet()

            // Debug post image URLs
            posts.forEach { post ->
                if (post.imageUrl != null) {
                    android.util.Log.d(
                        "FirebaseRepository",
                        "Post ID: ${post.id}, Image URL: ${post.imageUrl}"
                    )
                    try {
                        // Try to validate the image URL by checking if the file exists
                        val storageRef = storage.getReferenceFromUrl(post.imageUrl!!)
                        android.util.Log.d("FirebaseRepository", "Storage path: ${storageRef.path}")
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "FirebaseRepository",
                            "Invalid image URL: ${post.imageUrl}",
                            e
                        )
                    }
                }
            }

            // Mark posts as liked if they are in the userLikes set
            return@withContext posts.map { post ->
                post.copy(isLikedByCurrentUser = userLikes.contains(post.id))
            }
        } catch (e: Exception) {
            // Fallback in case of errors
            return@withContext emptyList()
       }
    }

    // Comment operations
    suspend fun addComment(postId: String, text: String, imageUri: Uri?): Comment = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
        
        // Get the user display name
        val userDisplayName = currentUser.displayName ?: "Unknown User"

        // If image is included, try to upload to storage first
        var imageUrl: String? = null
        if (imageUri != null) {
            try {
                imageUrl = uploadImage(imageUri, "comments")
            } catch (e: Exception) {
                // Log the error but continue creating comment without image
                android.util.Log.e(
                    "FirebaseRepository",
                    "Failed to upload comment image: ${e.message}"
                )
                // If image upload fails, we'll proceed with creating the comment without an image
            }
        }

        val commentId = commentsCollection.document().id
        val comment = Comment(
            id = commentId,
            postId = postId,
            userId = currentUser.uid,
            userDisplayName = userDisplayName, 
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
        try {
            commentsCollection
                .whereEqualTo("postId", postId)
                .get()
                .await()
                .toObjects(Comment::class.java)
                .sortedBy { it.createdAt }
        } catch (e: Exception) {
            // Fallback to a simpler query if index not available
            commentsCollection
                .get()
                .await()
                .toObjects(Comment::class.java)
                .filter { it.postId == postId }
                .sortedBy { it.createdAt }
        }
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

    // Comment like operations
    suspend fun toggleCommentLike(commentId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        val likeQuery = commentLikesCollection
            .whereEqualTo("commentId", commentId)
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .await()

        if (likeQuery.isEmpty) {
            // Add like
            val likeId = commentLikesCollection.document().id
            val likeData = hashMapOf(
                "id" to likeId,
                "commentId" to commentId,
                "userId" to currentUser.uid,
                "userDisplayName" to (currentUser.displayName ?: "Unknown User"),
                "createdAt" to Timestamp.now()
            )

            commentLikesCollection.document(likeId).set(likeData).await()
            commentsCollection.document(commentId).update("likeCount",
                com.google.firebase.firestore.FieldValue.increment(1)).await()
            true
        } else {
            // Remove like
            val likeDoc = likeQuery.documents.first()
            commentLikesCollection.document(likeDoc.id).delete().await()
            commentsCollection.document(commentId).update("likeCount",
                com.google.firebase.firestore.FieldValue.increment(-1)).await()
            false
        }
    }

    // Add a reply to a comment
    suspend fun addReply(parentCommentId: String, text: String, imageUri: Uri?): Comment = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
        val userDisplayName = currentUser.displayName ?: "Unknown User"

        // Get parent comment to find the post
        val parentComment = commentsCollection.document(parentCommentId).get().await()
            .toObject(Comment::class.java) ?: throw IllegalStateException("Parent comment not found")

        // If image is included, try to upload to storage first
        var imageUrl: String? = null
        if (imageUri != null) {
            try {
                imageUrl = uploadImage(imageUri, "comments")
            } catch (e: Exception) {
                // Log the error but continue creating reply without image
                android.util.Log.e(
                    "FirebaseRepository",
                    "Failed to upload reply image: ${e.message}"
                )
                // If image upload fails, we'll proceed with creating the reply without an image
            }
        }

        val commentId = commentsCollection.document().id
        val reply = Comment(
            id = commentId,
            postId = parentComment.postId, // Same post as parent
            userId = currentUser.uid,
            userDisplayName = userDisplayName,
            text = text,
            imageUrl = imageUrl,
            createdAt = Timestamp.now(),
            parentCommentId = parentCommentId // Mark as a reply
        )

        commentsCollection.document(commentId).set(reply).await()

        // Increment reply count on parent comment
        commentsCollection.document(parentCommentId).update("replyCount",
            com.google.firebase.firestore.FieldValue.increment(1)).await()

        reply
    }

    // Get replies to a specific comment
    suspend fun getRepliesForComment(commentId: String): List<Comment> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser
            
            // Get all replies for this comment
            val replies = commentsCollection
                .whereEqualTo("parentCommentId", commentId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Comment::class.java)
                
            // If user is logged in, check which replies they've liked
            if (currentUser != null) {
                val userLikes = commentLikesCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.getString("commentId") }
                    .toSet()
                    
                // Mark replies as liked if they're in userLikes
                return@withContext replies.map { reply ->
                    reply.copy(isLikedByCurrentUser = userLikes.contains(reply.id))
                }
            } else {
                return@withContext replies
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    // Helper method for image uploads
    private suspend fun uploadImage(imageUri: Uri, folder: String): String = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(
                "FirebaseRepository",
                "Using local URI as temporary solution: $imageUri"
            )
            // Simply return the local URI as a string - this will only work on the same device
            // and is NOT a production solution
            return@withContext imageUri.toString()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error processing local URI", e)
            throw Exception("Failed to process image: ${e.message}")
        }
    }
}