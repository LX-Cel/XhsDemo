package com.bytedance.xhsdemo.data

import android.content.Context
import androidx.core.content.edit
import androidx.appcompat.app.AppCompatDelegate

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit { putBoolean(KEY_LOGGED_IN, loggedIn) }
    }

    fun setThemeMode(mode: Int) {
        prefs.edit { putInt(KEY_THEME_MODE, mode) }
    }

    fun getThemeMode(): Int {
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_NO)
    }

    companion object {
        private const val PREF_NAME = "login_prefs"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
