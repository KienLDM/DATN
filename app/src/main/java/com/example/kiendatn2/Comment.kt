package com.example.kiendatn2

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now()
)