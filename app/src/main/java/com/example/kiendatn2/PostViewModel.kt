package com.example.kiendatn2

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PostViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _postsState = MutableLiveData<PostState>(PostState.Loading)
    val postsState: LiveData<PostState> = _postsState

    private val _commentsState = MutableLiveData<CommentsState>(CommentsState.Loading)
    val commentsState: LiveData<CommentsState> = _commentsState

    private val _currentPostState = MutableLiveData<CurrentPostState>(CurrentPostState.NotSelected)
    val currentPostState: LiveData<CurrentPostState> = _currentPostState

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
            try {
                repository.createPost(text, imageUri)
                loadPosts() // Refresh post list
            } catch (e: Exception) {
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