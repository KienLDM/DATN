package com.example.kiendatn2.data

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userDisplayName: String = "", // Include display name
    val text: String = "",
    val imageUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val likeCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val replyCount: Int = 0,
    val parentCommentId: String? = null // null for top-level comments, set for replies
)