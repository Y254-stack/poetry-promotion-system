package com.example.poetry.core.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistoryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_HISTORY = "history_list"
        private const val MAX_HISTORY_SIZE = 10
    }

    fun addSearchHistory(query: String) {
        if (query.isBlank()) return

        val history = getSearchHistory().toMutableList()

        // 如果已存在，先删除
        history.remove(query)

        // 添加到最前面
        history.add(0, query)

        // 限制数量
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }

        saveHistory(history)
    }

    fun getSearchHistory(): List<String> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun removeSearchHistory(query: String) {
        val history = getSearchHistory().toMutableList()
        history.remove(query)
        saveHistory(history)
    }

    fun clearAllHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistory(history: List<String>) {
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
}
