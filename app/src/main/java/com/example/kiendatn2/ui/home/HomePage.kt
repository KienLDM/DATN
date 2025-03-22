package com.example.kiendatn2.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.kiendatn2.data.Post
import com.example.kiendatn2.ui.auth.AuthState
import com.example.kiendatn2.ui.auth.AuthViewModel
import com.example.kiendatn2.ui.post.PostItemDetailed
import com.example.kiendatn2.ui.post.PostState
import com.example.kiendatn2.ui.post.PostViewModel

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
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(postsState.value) {
        if (postsState.value is PostState.Error) {
            errorMessage = (postsState.value as PostState.Error).message
            showError = true
        }
    }

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
                    
                    selectedImageUri?.let { uri ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Attach image"
                            )
                        }
                        
                        Button(
                            onClick = {
                                if (postText.isNotBlank() || selectedImageUri != null) {
                                    postViewModel.createPost(postText, selectedImageUri)
                                    postText = ""
                                    selectedImageUri = null
                                    showCreatePost = false
                                }
                            },
                            enabled = postText.isNotBlank() || selectedImageUri != null
                        ) {
                            Text("Post")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
                    TextButton(onClick = {
                        postViewModel.loadPosts()
                    }) {
                        Text("Retry")
                    }
                }
            }
            else -> {}
        }
    }

    // Show error dialog if needed
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun PostItem(post: Post, postViewModel: PostViewModel) {
    // State to track if post is being loaded for comments
    var isLoading by remember { mutableStateOf(false) }

    // Log image URL for debugging
    LaunchedEffect(post.id) {
        android.util.Log.d(
            "PostItem",
            "Post ID: ${post.id}, Has image URL: ${post.imageUrl != null}, Image URL: ${post.imageUrl ?: "none"}"
        )
    }

    PostItemDetailed(
        authorName = post.userDisplayName,  
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