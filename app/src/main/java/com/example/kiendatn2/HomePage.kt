package com.example.kiendatn2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel, postViewModel: PostViewModel) {
    val authState = authViewModel.authState.observeAsState()

    LaunchedEffect(authState.value) {
        when (authState.value){
            is AuthState.Unauthenticated -> navController.navigate("login")
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Social Feed") },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                    TextButton(onClick = { authViewModel.signout() }) {
                        Text("Sign out")
                    }
                }
            )
        }
    ) { paddingValues ->
        PostScreen(
            modifier = Modifier.padding(paddingValues),
            postViewModel = postViewModel
        )
    }
}

@Composable
fun PostScreen(modifier: Modifier = Modifier, postViewModel: PostViewModel) {
    val postsState = postViewModel.postsState.observeAsState()
    var showCreatePost by remember { mutableStateOf(false) }
    var postText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        if (showCreatePost) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Create Post",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = postText,
                        onValueChange = { postText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("What's on your mind?") },
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    IconButton(
                        onClick = {
                            if (postText.isNotBlank()) {
                                postViewModel.createPost(postText, null)
                                postText = ""
                                showCreatePost = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Post")
                    }
                }
            }
        } else {
            FloatingActionButton(
                onClick = { showCreatePost = true },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
        }

        when (val state = postsState.value) {
            is PostState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Loading posts...")
                }
            }
            is PostState.Success -> {
                if (state.posts.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "No posts yet. Be the first to post!")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(state.posts) { post ->
                            PostItem(post = post, postViewModel = postViewModel)
                        }
                    }
                }
            }
            is PostState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { postViewModel.loadPosts() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun PostItem(post: Post, postViewModel: PostViewModel) {
    // State to track if post is being loaded for comments
    var isLoading by remember { mutableStateOf(false) }
    
    PostItemDetailed(
        authorName = post.userId,  // Ideally this should display username instead of userId
        content = post.text,
        imageUrl = post.imageUrl,
        likeCount = post.likeCount,
        commentCount = post.commentCount,
        shareCount = post.shareCount,
        isLikedByCurrentUser = post.isLikedByCurrentUser,
        onLikeClick = { postViewModel.toggleLike(post.id) },
        onCommentClick = { 
            isLoading = true
            // Load post in the next frame to avoid UI glitches
            postViewModel.loadPostById(post.id)
        },
        onShareClick = { /* Implement share functionality */ }
    )
    
    // Show loading indicator if we're loading comments
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp)
            )
        }
    }
}