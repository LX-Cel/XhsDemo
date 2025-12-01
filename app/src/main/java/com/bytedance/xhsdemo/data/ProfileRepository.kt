package com.bytedance.xhsdemo.data

import com.bytedance.xhsdemo.data.local.ProfileDao
import com.bytedance.xhsdemo.data.local.UserProfileEntity

class ProfileRepository(private val profileDao: ProfileDao) {

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

    suspend fun updateProfile(name: String, signature: String) {
        val current = loadProfile()
        profileDao.updateProfile(
            current.copy(displayName = name, signature = signature)
        )
    }

    suspend fun updateAvatar(avatarUrl: String) {
        val current = loadProfile()
        profileDao.updateProfile(current.copy(avatarUrl = avatarUrl))
    }

    suspend fun updateName(name: String) {
        val current = loadProfile()
        profileDao.updateProfile(current.copy(displayName = name))
    }
}
