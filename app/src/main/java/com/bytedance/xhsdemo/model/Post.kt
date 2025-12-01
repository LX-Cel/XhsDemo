package com.bytedance.xhsdemo.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Comment(
    val id: String,
    val userName: String,
    val userAvatar: String,
    val content: String
) : Parcelable

@Parcelize
data class Post(
    val id: String,
    val title: String,
    val content: String,
    val imageUrl: String,
    val authorName: String,
    val authorAvatar: String,
    val publishTime: String,
    val likes: Int,
    val comments: List<Comment> = emptyList()
) : Parcelable
