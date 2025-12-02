package com.bytedance.xhsdemo.data

import com.bytedance.xhsdemo.data.local.ProfileDao
import com.bytedance.xhsdemo.data.local.UserProfileEntity

// 个人资料仓库：负责从本地数据库读取/更新用户主页信息
class ProfileRepository(private val profileDao: ProfileDao) {

    // 加载个人资料；若不存在则创建一份默认资料
    suspend fun loadProfile(): UserProfileEntity {
        val cached = profileDao.getProfile()
        if (cached != null) return cached

        val defaultProfile = UserProfileEntity(
            displayName = "Sherry ~",
            signature = "点击这里，填写简介",
            avatarUrl = ""
        )
        profileDao.insertProfile(defaultProfile)
        return defaultProfile
    }

    // 同时更新昵称和签名
    suspend fun updateProfile(name: String, signature: String) {
        val current = loadProfile()
        profileDao.updateProfile(
            current.copy(displayName = name, signature = signature)
        )
    }

    // 更新头像地址
    suspend fun updateAvatar(avatarUrl: String) {
        val current = loadProfile()
        profileDao.updateProfile(current.copy(avatarUrl = avatarUrl))
    }

    // 单独更新昵称
    suspend fun updateName(name: String) {
        val current = loadProfile()
        profileDao.updateProfile(current.copy(displayName = name))
    }
}
