package com.example.kiendatn2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    postViewModel: PostViewModel = viewModel(),
    onBackClick: () -> Unit
) {
   val currentPostState = postViewModel.currentPostState.observeAsState().value
   val commentsState = postViewModel.commentsState.observeAsState().value
   val keyboardController = LocalSoftwareKeyboardController.current
   val focusManager = LocalFocusManager.current
   var commentText by remember { mutableStateOf("") }
   
   Scaffold(
       topBar = {
           TopAppBar(
               title = { Text("Post Details") },
               navigationIcon = {
                   IconButton(onClick = {
                       postViewModel.clearCurrentPost()
                       onBackClick()
                   }) {
                       Icon(
                           imageVector = Icons.Default.ArrowBack, 
                           contentDescription = "Back"
                       )
                   }
               }
           )
       }
   ) { paddingValues ->
       Box(
           modifier = Modifier
               .fillMaxSize()
               .padding(paddingValues)
       ) {
           when (currentPostState) {
               is CurrentPostState.Loading -> {
                   CircularProgressIndicator(
                       modifier = Modifier.align(Alignment.Center)
                   )
               }
               is CurrentPostState.PostLoaded -> {
                   val post = currentPostState.post
                   
                   Box(modifier = Modifier.fillMaxSize()) {
                       // Main content - scrollable
                       Column(
                           modifier = Modifier
                               .fillMaxSize()
                               .padding(bottom = 80.dp) // Add space for comment input
                       ) {
                           // Post details
                           PostItemDetailed(
                               authorName = post.userId,
                               content = post.text,
                               imageUrl = post.imageUrl,
                               likeCount = post.likeCount,
                               commentCount = post.commentCount,
                               shareCount = post.shareCount,
                               isLikedByCurrentUser = post.isLikedByCurrentUser,
                               onLikeClick = { postViewModel.toggleLike(post.id) },
                               onCommentClick = { /* Already on comment screen */ },
                               onShareClick = { /* Share functionality */ }
                           )
                           
                           Spacer(modifier = Modifier.height(8.dp))
                           
                           // Comments section divider
                           Divider(
                               color = Color.LightGray,
                               thickness = 4.dp
                           )
                           
                           Text(
                               text = "Comments",
                               style = MaterialTheme.typography.titleLarge,
                               modifier = Modifier.padding(16.dp)
                           )
                           
                           // Display comments
                           when (commentsState) {
                               is CommentsState.Loading -> {
                                   Box(
                                       modifier = Modifier
                                           .fillMaxWidth()
                                           .padding(16.dp),
                                       contentAlignment = Alignment.Center
                                   ) {
                                       CircularProgressIndicator()
                                   }
                               }
                               is CommentsState.Success -> {
                                   val comments = commentsState.comments
                                   
                                   if (comments.isEmpty()) {
                                       Box(
                                           modifier = Modifier
                                               .fillMaxWidth()
                                               .padding(32.dp),
                                           contentAlignment = Alignment.Center
                                       ) {
                                           Text("No comments yet. Be the first to comment!")
                                       }
                                   } else {
                                       LazyColumn {
                                           items(comments) { comment ->
                                               CommentItem(comment = comment)
                                           }
                                       }
                                   }
                               }
                               is CommentsState.Error -> {
                                   Text(
                                       text = "Error loading comments: ${commentsState.message}",
                                       color = Color.Red,
                                       modifier = Modifier.padding(16.dp)
                                   )
                               }
                               null -> {
                                   Text(
                                       text = "Loading comments...",
                                       modifier = Modifier.padding(16.dp)
                                   )
                               }
                           }
                       }
                       
                       // Comment input section - fixed at bottom
                       Card(
                           modifier = Modifier
                               .align(Alignment.BottomCenter)
                               .fillMaxWidth(),
                           elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                       ) {
                           Row(
                               modifier = Modifier
                                   .fillMaxWidth()
                                   .padding(12.dp),
                               verticalAlignment = Alignment.CenterVertically
                           ) {
                               OutlinedTextField(
                                   value = commentText,
                                   onValueChange = { commentText = it },
                                   modifier = Modifier.weight(1f),
                                   placeholder = { Text("Add a comment...") },
                                   maxLines = 3
                               )
                               
                               Spacer(modifier = Modifier.width(8.dp))
                               
                               IconButton(
                                   onClick = {
                                       if (commentText.isNotBlank()) {
                                           postViewModel.addComment(post.id, commentText, null)
                                           commentText = ""
                                           keyboardController?.hide()
                                           focusManager.clearFocus()
                                       }
                                   }
                               ) {
                                   Icon(
                                       imageVector = Icons.Default.Send,
                                       contentDescription = "Send comment",
                                       tint = if (commentText.isNotBlank()) 
                                           MaterialTheme.colorScheme.primary 
                                       else 
                                           MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                   )
                               }
                           }
                       }
                   }
               }
               is CurrentPostState.Error -> {
                   Text(
                       text = "Error: ${currentPostState.message}",
                       color = Color.Red,
                       modifier = Modifier
                           .align(Alignment.Center)
                           .padding(16.dp)
                   )
               }
               CurrentPostState.NotSelected -> {
                   Text(
                       text = "No post selected",
                       modifier = Modifier
                           .align(Alignment.Center)
                           .padding(16.dp)
                   )
               }
               null -> {
                   CircularProgressIndicator(
                       modifier = Modifier.align(Alignment.Center)
                   )
               }
           }
       }
   }
}

@Composable
fun CommentItem(comment: Comment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "User ${comment.userId.take(5)}",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}