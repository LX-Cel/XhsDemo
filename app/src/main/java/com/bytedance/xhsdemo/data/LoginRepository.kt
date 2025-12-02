package com.bytedance.xhsdemo.data

import com.bytedance.xhsdemo.data.local.UserDao
import com.bytedance.xhsdemo.data.local.UserEntity

// 登录数据仓库：封装本地 UserDao 与 SessionManager，负责登录校验与默认用户初始化
class LoginRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
    ) {

    // 确保本地有一个默认账号，方便开发调试
    suspend fun ensureDefaultUser() {
        val defaultPhone = "13800138000"
        val defaultPassword = "123456"
        val exists = userDao.findUser(defaultPhone)
        if (exists == null) {
            userDao.insertUser(UserEntity(defaultPhone, defaultPassword))
        }
    }

    // 执行登录逻辑：校验账号密码，并在成功时写入登录状态
    suspend fun login(phone: String, password: String): Result<Unit> {
        val user = userDao.findUser(phone.trim())
        return if (user != null && user.password == password.trim()) {
            sessionManager.setLoggedIn(true)
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("账号或密码错误"))
        }
    }

    // 若本地不存在该账号，则自动注册一条用户记录
    suspend fun registerIfMissing(phone: String, password: String) {
        val user = userDao.findUser(phone)
        if (user == null) {
            userDao.insertUser(UserEntity(phone, password))
        }
    }
}
