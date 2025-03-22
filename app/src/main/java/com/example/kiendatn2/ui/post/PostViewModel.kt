package com.example.kiendatn2.ui.post

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiendatn2.repository.FirebaseRepository
import com.example.kiendatn2.data.Comment
import com.example.kiendatn2.data.Post
import kotlinx.coroutines.launch

class PostViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _postsState = MutableLiveData<PostState>(PostState.Loading)
    val postsState: LiveData<PostState> = _postsState

    private val _commentsState = MutableLiveData<CommentsState>(CommentsState.Loading)
    val commentsState: LiveData<CommentsState> = _commentsState

    private val _currentPostState = MutableLiveData<CurrentPostState>(CurrentPostState.NotSelected)
    val currentPostState: LiveData<CurrentPostState> = _currentPostState

    private val _repliesState = MutableLiveData<Map<String, List<Comment>>>(emptyMap())
    val repliesState: LiveData<Map<String, List<Comment>>> = _repliesState

    private val _replyingToState = MutableLiveData<Comment?>(null)
    val replyingToState: LiveData<Comment?> = _replyingToState

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _postsState.value = PostState.Loading
            try {
                val posts = repository.getPostsWithLikeStatus()
                _postsState.value = PostState.Success(posts)
            } catch (e: Exception) {
                _postsState.value = PostState.Error(e.message ?: "Error loading posts")
            }
        }
    }

    fun createPost(text: String, imageUri: Uri?) {
        viewModelScope.launch {
            // Show loading state
            _postsState.value = PostState.Loading
            try {
                repository.createPost(text, imageUri)
                loadPosts() // Refresh post list
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error creating post", e)
                _postsState.value = PostState.Error(e.message ?: "Failed to create post")
            }
        }
    }

    fun getCommentsForPost(postId: String) {
        viewModelScope.launch {
            _commentsState.value = CommentsState.Loading
            try {
                val comments = repository.getCommentsForPost(postId)
                _commentsState.value = CommentsState.Success(comments)
            } catch (e: Exception) {
                _commentsState.value = CommentsState.Error(e.message ?: "Error loading comments")
            }
        }
    }

    fun addComment(postId: String, text: String, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                repository.addComment(postId, text, imageUri)
                getCommentsForPost(postId) // Refresh comments
            } catch (e: Exception) {
                _commentsState.value = CommentsState.Error(e.message ?: "Failed to add comment")
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                val isLiked = repository.toggleLike(postId)
                
                // Update the post in the current state to avoid full reload
                val currentPosts = when (val state = _postsState.value) {
                    is PostState.Success -> state.posts
                    else -> emptyList()
                }
                
                val updatedPosts = currentPosts.map { post ->
                    if (post.id == postId) {
                        // Update like count and status
                        val newLikeCount = if (isLiked) post.likeCount + 1 else post.likeCount - 1
                        post.copy(
                            likeCount = newLikeCount.coerceAtLeast(0),
                            isLikedByCurrentUser = isLiked
                        )
                    } else post
                }
                
                _postsState.value = PostState.Success(updatedPosts)
                
                // If we're viewing a specific post, update that too
                if (_currentPostState.value is CurrentPostState.PostLoaded &&
                    (_currentPostState.value as CurrentPostState.PostLoaded).post.id == postId) {
                    loadPostById(postId)
                }
            } catch (e: Exception) {
                _postsState.value = PostState.Error(e.message ?: "Failed to toggle like")
            }
        }
    }

    fun loadPostById(postId: String) {
        viewModelScope.launch {
            _currentPostState.value = CurrentPostState.Loading
            try {
                val post = repository.getPostById(postId)
                if (post != null) {
                    _currentPostState.value = CurrentPostState.PostLoaded(post)
                    getCommentsForPost(postId) // Also load comments
                } else {
                    _currentPostState.value = CurrentPostState.Error("Post not found")
                }
            } catch (e: Exception) {
                _currentPostState.value = CurrentPostState.Error(e.message ?: "Error loading post")
            }
        }
    }
    
    fun clearCurrentPost() {
        _currentPostState.value = CurrentPostState.NotSelected
    }

    fun toggleCommentLike(commentId: String) {
        viewModelScope.launch {
            try {
                val isLiked = repository.toggleCommentLike(commentId)
                
                // Update comments state to reflect the like change
                _commentsState.value?.let { state ->
                    if (state is CommentsState.Success) {
                        val updatedComments = state.comments.map { comment ->
                            if (comment.id == commentId) {
                                val newLikeCount = if (isLiked) {
                                    comment.likeCount + 1
                                } else {
                                    (comment.likeCount - 1).coerceAtLeast(0) // Ensure not negative
                                }
                                comment.copy(
                                    likeCount = newLikeCount,
                                    isLikedByCurrentUser = isLiked
                                )
                            } else {
                                comment
                            }
                        }
                        _commentsState.value = CommentsState.Success(updatedComments)
                    }
                }
                
                // Also update any replies if we're showing them
                _repliesState.value?.let { repliesMap ->
                    val updatedRepliesMap = repliesMap.mapValues { (_, replies) ->
                        replies.map { reply ->
                            if (reply.id == commentId) {
                                val newLikeCount = if (isLiked) {
                                    reply.likeCount + 1
                                } else {
                                    (reply.likeCount - 1).coerceAtLeast(0)
                                }
                                reply.copy(
                                    likeCount = newLikeCount,
                                    isLikedByCurrentUser = isLiked
                                )
                            } else {
                                reply
                            }
                        }
                    }
                    _repliesState.value = updatedRepliesMap
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error toggling comment like", e)
            }
        }
    }

    fun addReply(parentCommentId: String, text: String, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                val reply = repository.addReply(parentCommentId, text, imageUri)
                
                // Update the parent comment's reply count
                _commentsState.value?.let { state ->
                    if (state is CommentsState.Success) {
                        val updatedComments = state.comments.map { comment ->
                            if (comment.id == parentCommentId) {
                                comment.copy(replyCount = comment.replyCount + 1)
                            } else {
                                comment
                            }
                        }
                        _commentsState.value = CommentsState.Success(updatedComments)
                    }
                }
                
                // Add the reply to replies state if we're showing replies for this comment
                val currentReplies = _repliesState.value ?: emptyMap()
                val commentReplies = currentReplies[parentCommentId] ?: emptyList()
                _repliesState.value = currentReplies + (parentCommentId to commentReplies + reply)
                
                // Clear the replying state
                _replyingToState.value = null
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error adding reply", e)
            }
        }
    }

    fun loadReplies(commentId: String) {
        viewModelScope.launch {
            try {
                val replies = repository.getRepliesForComment(commentId)
                
                // Update replies state
                val currentReplies = _repliesState.value ?: emptyMap()
                _repliesState.value = currentReplies + (commentId to replies)
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error loading replies", e)
            }
        }
    }

    fun setReplyingTo(comment: Comment) {
        _replyingToState.value = comment
    }

    fun cancelReply() {
        _replyingToState.value = null
    }
}

sealed class PostState {
    object Loading : PostState()
    data class Success(val posts: List<Post>) : PostState()
    data class Error(val message: String) : PostState()
}

sealed class CommentsState {
    object Loading : CommentsState()
    data class Success(val comments: List<Comment>) : CommentsState()
    data class Error(val message: String) : CommentsState()
}

sealed class CurrentPostState {
    object NotSelected : CurrentPostState()
    object Loading : CurrentPostState()
    data class PostLoaded(val post: Post) : CurrentPostState()
    data class Error(val message: String) : CurrentPostState()
}