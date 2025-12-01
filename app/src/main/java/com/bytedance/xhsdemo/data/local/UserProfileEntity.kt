package com.bytedance.xhsdemo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String,
    val signature: String,
    val avatarUrl: String
)
