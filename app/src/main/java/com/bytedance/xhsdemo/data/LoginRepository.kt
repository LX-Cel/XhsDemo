package com.bytedance.xhsdemo.data

import com.bytedance.xhsdemo.data.local.UserDao
import com.bytedance.xhsdemo.data.local.UserEntity

class LoginRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
    ) {

    suspend fun ensureDefaultUser() {
        val defaultPhone = "13800138000"
        val defaultPassword = "123456"
        val exists = userDao.findUser(defaultPhone)
        if (exists == null) {
            userDao.insertUser(UserEntity(defaultPhone, defaultPassword))
        }
    }

    suspend fun login(phone: String, password: String): Result<Unit> {
        val user = userDao.findUser(phone.trim())
        return if (user != null && user.password == password.trim()) {
            sessionManager.setLoggedIn(true)
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("账号或密码错误"))
        }
    }

    suspend fun registerIfMissing(phone: String, password: String) {
        val user = userDao.findUser(phone)
        if (user == null) {
            userDao.insertUser(UserEntity(phone, password))
        }
    }
}
