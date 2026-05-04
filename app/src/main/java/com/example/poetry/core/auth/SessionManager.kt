package com.example.poetry.core.auth

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("poetry_session", Context.MODE_PRIVATE)

    fun saveSession(token: String, userId: Long, username: String, nickname: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_NICKNAME, nickname)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun token(): String? = prefs.getString(KEY_TOKEN, null)

    fun nickname(): String? = prefs.getString(KEY_NICKNAME, null)

    fun username(): String? = prefs.getString(KEY_USERNAME, null)

    fun userId(): Long = prefs.getLong(KEY_USER_ID, 0L)

    fun isLoggedIn(): Boolean = !token().isNullOrBlank()

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_NICKNAME = "nickname"
    }
}
