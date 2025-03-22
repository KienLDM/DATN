package com.example.kiendatn2.ui.post

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.kiendatn2.R

@Composable
fun PostItemDetailed(
    authorName: String,
    content: String,
    imageUrl: String? = null,
    likeCount: Int = 0,
    isLikedByCurrentUser: Boolean = false,
    commentCount: Int = 0,
    shareCount: Int = 0,
    profilePictureUrl: String? = null,
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    // Use the authorName directly instead of modifying it
    // (since it will now come from userDisplayName)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Author info row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                // Profile picture
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    if (profilePictureUrl != null) {
                        AsyncImage(
                            model = profilePictureUrl,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // User initial as placeholder - use first letter of display name
                        Text(
                            text = authorName.firstOrNull()?.toString() ?: "?",
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Author name - use display name directly
                Text(
                    text = authorName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )
            }
            
            // Post content
            Text(
                text = content,
                fontSize = 20.sp,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (imageUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(bottom = 5.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                    )

                    AsyncImage(
                        model = if (imageUrl.startsWith("content:") || imageUrl.startsWith("file:")) {
                            Uri.parse(imageUrl)
                        } else {
                            imageUrl
                        },
                        contentDescription = "Post image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.ic_error_image),
                        placeholder = painterResource(id = R.drawable.ic_error_image),
                        onLoading = {
                            android.util.Log.d("PostItemDetailed", "Loading image: $imageUrl")
                        },
                        onError = {
                            android.util.Log.e(
                                "PostItemDetailed",
                                "Error loading image: $imageUrl, error: $it"
                            )
                        },
                        onSuccess = {
                            android.util.Log.d(
                                "PostItemDetailed",
                                "Successfully loaded image: $imageUrl"
                            )
                        }
                    )
                }
            }
            
            Divider(
                modifier = Modifier.padding(horizontal = 8.dp),
                color = Color.LightGray,
                thickness = 0.5.dp
            )

            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceAround
            ) {
                // Like button
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .clickable {
                            onLikeClick()
                        },
                    contentAlignment = Alignment.CenterStart
               ) {
                   Row(
                       verticalAlignment = Alignment.CenterVertically
                   ) {
                       Image(
                           painter = painterResource(
                               if (isLikedByCurrentUser) R.drawable.ic_like_filled
                               else R.drawable.ic_like
                           ),
                           contentDescription = "Like icon",
                           modifier = Modifier
                               .size(24.dp)
                               .padding(end = 4.dp)
                       )

                       Text(
                           text = "$likeCount",
                           fontSize = 16.sp,
                           color = if (isLikedByCurrentUser) Color(0xFFE91E63) else Color.Black,
                           modifier = Modifier
                       )
                   }
                }

                // Comment button
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .clickable {
                            onCommentClick()
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                   Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                       Image(
                           painter = painterResource(R.drawable.ic_comment),
                           contentDescription = "Comment icon",
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp)
                       )

                       Text(
                           text = "$commentCount",
                           fontSize = 16.sp,
                           color = Color.Black,
                            modifier = Modifier
                       )
                   }
               }

                // Share button
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .clickable {
                            onShareClick()
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                   Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                       Image(
                           painter = painterResource(R.drawable.ic_share),
                           contentDescription = "Share icon", 
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp)
                       )

                       Text(
                           text = "$shareCount",
                           fontSize = 16.sp,
                           color = Color.Black,
                            modifier = Modifier
                       )
                   }
                }
            }
            
            // Optional bottom padding for better spacing
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PostItemDetailedPreview() {
    PostItemDetailed(
        authorName = "John Doe",
        content = "This is a sample post content showing how the layout looks with some text.",
        likeCount = 42,
        commentCount = 7,
        shareCount = 3
    )
}