package com.example.poetry.features.learning.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poetry.core.network.FeihuaRequest
import com.example.poetry.core.network.NetworkModule
import kotlinx.coroutines.launch

class FeihuaViewModel : ViewModel() {

    private val _keyword = MutableLiveData("")
    val keyword: LiveData<String> = _keyword

    private val _chatMessages = MutableLiveData<List<ChatMessage>>(emptyList())
    val chatMessages: LiveData<List<ChatMessage>> = _chatMessages

    private val _isPlayerTurn = MutableLiveData(true)
    val isPlayerTurn: LiveData<Boolean> = _isPlayerTurn

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _gameOver = MutableLiveData(false)
    val gameOver: LiveData<Boolean> = _gameOver

    private val _winner = MutableLiveData<String?>(null)
    val winner: LiveData<String?> = _winner

    private val usedLines = mutableListOf<String>()

    private val keywords = listOf("花", "月", "山", "水", "春", "秋", "江", "雪", "风", "雨")

    init {
        randomKeyword()
    }

    fun randomKeyword() {
        _keyword.value = keywords.random()
        resetGame()
        addMessage("系统", "🎯 新的一局！关键字：【${_keyword.value}】，你先说～")
    }

    fun resetGame() {
        usedLines.clear()
        _chatMessages.value = emptyList()
        _isPlayerTurn.value = true
        _gameOver.value = false
        _winner.value = null
    }

    fun changeKeyword() {
        randomKeyword()
    }

    suspend fun submitAnswer(userLine: String): Boolean {
        if (_gameOver.value == true) return false
        if (_isPlayerTurn.value != true) return false

        _isLoading.value = true

        return try {
            val request = FeihuaRequest(
                keyword = _keyword.value!!,
                userLine = userLine,
                usedLines = usedLines.toList()
            )

            val result = NetworkModule.poetryApiService.judgeFeihua(request)

            if (result.valid && result.hasKeyword && !result.isDuplicate) {
                // 正确
                usedLines.add(userLine)
                addMessage("我", userLine)

                // 检查是否赢了（如果AI接下来认输）
                val aiResult = aiTurn()

                _isLoading.value = false

                if (aiResult == "认输") {
                    gameEnd("我")
                }
                true
            } else {
                addMessage("系统", result.message)
                _isLoading.value = false
                false
            }
        } catch (e: Exception) {
            addMessage("系统", "网络错误：${e.message}")
            _isLoading.value = false
            false
        }
    }

    private suspend fun aiTurn(): String {
        _isPlayerTurn.value = false
        addMessage("AI", "思考中...")

        return try {
            val request = FeihuaRequest(
                keyword = _keyword.value!!,
                userLine = null,
                usedLines = usedLines.toList()
            )

            val response = NetworkModule.poetryApiService.aiTurn(request)
            val aiLine = response.line

            if (aiLine == "认输" || aiLine.isEmpty()) {
                addMessage("AI", "我想不出来了... 认输！")
                "认输"
            } else {
                usedLines.add(aiLine)
                // 替换最后一条"思考中..."消息
                val currentMessages = _chatMessages.value?.toMutableList() ?: mutableListOf()
                if (currentMessages.isNotEmpty() && currentMessages.last().message == "思考中...") {
                    currentMessages.removeAt(currentMessages.size - 1)
                }
                currentMessages.add(ChatMessage("AI", aiLine))
                _chatMessages.value = currentMessages
                _isPlayerTurn.value = true
                aiLine
            }
        } catch (e: Exception) {
            addMessage("系统", "AI出错：${e.message}")
            _isPlayerTurn.value = true
            ""
        }
    }

    private fun gameEnd(winner: String) {
        _gameOver.value = true
        _winner.value = winner
        _isPlayerTurn.value = false
        addMessage("系统", "🏆 游戏结束！${winner} 获胜！🏆")
    }

    private fun addMessage(sender: String, message: String) {
        val current = _chatMessages.value?.toMutableList() ?: mutableListOf()
        current.add(ChatMessage(sender, message))
        _chatMessages.value = current
    }

    data class ChatMessage(val sender: String, val message: String)
}