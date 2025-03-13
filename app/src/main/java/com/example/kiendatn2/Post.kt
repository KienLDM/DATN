package com.example.kiendatn2

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)