package com.example.poetry.core.auth

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedLoginAccount(
    val userId: Long,
    val username: String,
    val nickname: String,
    val token: String,
    val avatarUrl: String? = null
)

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(
        token: String,
        userId: Long,
        username: String,
        nickname: String,
        avatarUrl: String?
    ) {
        val editor = prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_NICKNAME, nickname)
        if (avatarUrl != null) {
            editor.putString(KEY_AVATAR_URL, avatarUrl)
        } else {
            editor.remove(KEY_AVATAR_URL)
        }
        editor.apply()
        upsertSavedAccount(SavedLoginAccount(userId, username, nickname, token, avatarUrl))
    }

    fun clearActiveSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .putLong(KEY_USER_ID, 0L)
            .remove(KEY_USERNAME)
            .remove(KEY_NICKNAME)
            .remove(KEY_AVATAR_URL)
            .apply()
    }

    /** 清除当前会话及所有已保存账号（注销本地数据等场景）。 */
    fun clearEverything() {
        prefs.edit().clear().apply()
    }

    fun token(): String? = prefs.getString(KEY_TOKEN, null)

    fun nickname(): String? = prefs.getString(KEY_NICKNAME, null)

    fun username(): String? = prefs.getString(KEY_USERNAME, null)

    fun avatarUrl(): String? = prefs.getString(KEY_AVATAR_URL, null)

    fun userId(): Long = prefs.getLong(KEY_USER_ID, 0L)

    fun isLoggedIn(): Boolean = !token().isNullOrBlank()

    /** Retrofit / Spring: Authorization: Bearer <token> */
    fun bearerAuthorization(): String? {
        val t = token()?.trim().orEmpty()
        return if (t.isEmpty()) null else "Bearer $t"
    }

    fun listSavedLoginAccounts(): List<SavedLoginAccount> {
        val arr = readSavedJsonArray()
        val out = ArrayList<SavedLoginAccount>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optLong("userId", 0L)
            val tok = o.optString("token", "")
            if (id > 0 && tok.isNotBlank()) {
                out.add(
                    SavedLoginAccount(
                        userId = id,
                        username = o.optString("username", ""),
                        nickname = o.optString("nickname", ""),
                        token = tok,
                        avatarUrl = o.optString("avatarUrl", "").ifBlank { null }
                    )
                )
            }
        }
        return out
    }

    fun switchToAccount(userId: Long): Boolean {
        val acc = listSavedLoginAccounts().find { it.userId == userId } ?: return false
        val editor = prefs.edit()
            .putString(KEY_TOKEN, acc.token)
            .putLong(KEY_USER_ID, acc.userId)
            .putString(KEY_USERNAME, acc.username)
            .putString(KEY_NICKNAME, acc.nickname)
        if (acc.avatarUrl != null) {
            editor.putString(KEY_AVATAR_URL, acc.avatarUrl)
        } else {
            editor.remove(KEY_AVATAR_URL)
        }
        editor.apply()
        return true
    }

    fun removeSavedAccount(userId: Long) {
        val arr = readSavedJsonArray()
        val next = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optLong("userId", 0L) != userId) {
                next.put(o)
            }
        }
        prefs.edit().putString(KEY_SAVED_ACCOUNTS, next.toString()).apply()
        if (userId == prefs.getLong(KEY_USER_ID, 0L)) {
            clearActiveSession()
        }
    }

    private fun upsertSavedAccount(account: SavedLoginAccount) {
        val arr = readSavedJsonArray()
        val next = JSONArray()
        var replaced = false
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optLong("userId", 0L) == account.userId) {
                next.put(account.toJson())
                replaced = true
            } else {
                next.put(o)
            }
        }
        if (!replaced) {
            next.put(account.toJson())
        }
        prefs.edit().putString(KEY_SAVED_ACCOUNTS, next.toString()).apply()
    }

    private fun SavedLoginAccount.toJson(): JSONObject = JSONObject().apply {
        put("userId", userId)
        put("username", username)
        put("nickname", nickname)
        put("token", token)
        if (!avatarUrl.isNullOrBlank()) {
            put("avatarUrl", avatarUrl)
        }
    }

    private fun readSavedJsonArray(): JSONArray {
        val raw = prefs.getString(KEY_SAVED_ACCOUNTS, null) ?: "[]"
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    companion object {
        private const val PREFS_NAME = "poetry_session"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_SAVED_ACCOUNTS = "saved_login_accounts_json"
    }
}
