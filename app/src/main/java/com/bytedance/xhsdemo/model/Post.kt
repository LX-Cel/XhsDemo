package com.bytedance.xhsdemo.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// 评论模型：用于详情页评论列表展示，也作为 Post 的子对象
@Parcelize
data class Comment(
    val id: String,
    val userName: String,
    val userAvatar: String,
    val content: String
) : Parcelable

// 帖子模型：可通过 Parcelable 在 Activity/Fragment 之间传递
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
