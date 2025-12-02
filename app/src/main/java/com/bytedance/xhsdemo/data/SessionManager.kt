package com.bytedance.xhsdemo.data

import android.content.Context
import androidx.core.content.edit
import androidx.appcompat.app.AppCompatDelegate

// 会话管理：封装 SharedPreferences，用于保存登录状态和主题模式等应用级设置
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // 是否已经登录
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    // 更新登录状态
    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit { putBoolean(KEY_LOGGED_IN, loggedIn) }
    }

    // 保存当前主题模式（跟随系统/明亮/黑暗）
    fun setThemeMode(mode: Int) {
        prefs.edit { putInt(KEY_THEME_MODE, mode) }
    }

    // 读取主题模式，默认日间模式
    fun getThemeMode(): Int {
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_NO)
    }

    companion object {
        private const val PREF_NAME = "login_prefs"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
