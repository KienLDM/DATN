package com.example.kiendatn2.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiendatn2.data.User
import com.example.kiendatn2.repository.FirebaseRepository
import com.example.kiendatn2.data.Post
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    
    private val _profileState = MutableLiveData<ProfileState>()
    val profileState: LiveData<ProfileState> = _profileState
    
    private val _userPostsState = MutableLiveData<UserPostsState>()
    val userPostsState: LiveData<UserPostsState> = _userPostsState
    
    fun loadUserProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val user = repository.getCurrentUser()
                if (user != null) {
                    _profileState.value = ProfileState.Success(user)
                    loadUserPosts() // Load user posts after profile is loaded
                } else {
                    // User profile doesn't exist, create one
                    try {
                        val currentFirebaseUser = repository.getCurrentFirebaseUser()
                        if (currentFirebaseUser != null) {
                            // Create a basic profile from Firebase auth data
                            val newUser = User(
                                id = currentFirebaseUser.uid,
                                displayName = currentFirebaseUser.displayName ?: currentFirebaseUser.email?.substringBefore('@') ?: "User",
                                email = currentFirebaseUser.email ?: ""
                            )
                            repository.createUserProfile(newUser)
                            _profileState.value = ProfileState.Success(newUser)
                            loadUserPosts()
                        } else {
                            _profileState.value = ProfileState.Error("Not signed in")
                        }
                    } catch (e: Exception) {
                        _profileState.value =
                            ProfileState.Error("Failed to create user profile: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Error loading profile")
            }
        }
    }
    
    fun loadUserPosts() {
        viewModelScope.launch {
            _userPostsState.value = UserPostsState.Loading
            try {
                val posts = repository.getUserPosts()
                _userPostsState.value = UserPostsState.Success(posts)
            } catch (e: Exception) {
                // If we fail to load posts, show an empty list rather than an error
                _userPostsState.value = UserPostsState.Success(emptyList())
            }
        }
    }
    
    fun updateProfile(displayName: String, bio: String?, photoUri: Uri?) {
        viewModelScope.launch {
            try {
                repository.updateUserProfile(displayName, bio, photoUri)
                loadUserProfile() // Reload profile after update
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Error updating profile")
            }
        }
    }
}

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class UserPostsState {
    object Loading : UserPostsState()
    data class Success(val posts: List<Post>) : UserPostsState()
    data class Error(val message: String) : UserPostsState()
}