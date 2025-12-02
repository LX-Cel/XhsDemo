package com.bytedance.xhsdemo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// 用户资料表实体：固定使用 id=1 代表当前登录用户
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String,
    val signature: String,
    val avatarUrl: String
)
