package com.bytedance.xhsdemo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// 用户表实体：以手机号作为主键
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val phone: String,
    val password: String
)
