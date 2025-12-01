package com.bytedance.xhsdemo.data

import com.bytedance.xhsdemo.data.local.ProfileDao
import com.bytedance.xhsdemo.data.local.UserProfileEntity

class ProfileRepository(private val profileDao: ProfileDao) {

    suspend fun loadProfile(): UserProfileEntity? = profileDao.getProfile()

    suspend fun updateProfile(name: String, signature: String) {
        val current = profileDao.getProfile()
        if (current != null) {
            profileDao.updateProfile(
                current.copy(displayName = name, signature = signature)
            )
        }
    }

    suspend fun updateAvatar(avatarUrl: String) {
        val current = profileDao.getProfile()
        if (current != null) {
            profileDao.updateProfile(current.copy(avatarUrl = avatarUrl))
        }
    }

    suspend fun updateName(name: String) {
        val current = profileDao.getProfile()
        if (current != null) {
            profileDao.updateProfile(current.copy(displayName = name))
        }
    }
}
