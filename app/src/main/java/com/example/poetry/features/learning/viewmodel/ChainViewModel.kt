package com.example.poetry.features.learning.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poetry.core.network.ChainRequest
import com.example.poetry.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChainViewModel : ViewModel() {

    // 当前接龙字（尾字）
    private val _currentChar = MutableLiveData("")
    val currentChar: LiveData<String> = _currentChar

    // 聊天消息列表
    private val _chatMessages = MutableLiveData<List<ChatMessage>>(emptyList())
    val chatMessages: LiveData<List<ChatMessage>> = _chatMessages

    // 是否轮到玩家
    private val _isPlayerTurn = MutableLiveData(true)
    val isPlayerTurn: LiveData<Boolean> = _isPlayerTurn

    // 是否加载中
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // 游戏是否结束
    private val _gameOver = MutableLiveData(false)
    val gameOver: LiveData<Boolean> = _gameOver

    // 胜利者
    private val _winner = MutableLiveData<String?>(null)
    val winner: LiveData<String?> = _winner

    // 接龙轮次
    private val _roundCount = MutableLiveData(0)
    val roundCount: LiveData<Int> = _roundCount

    // 游戏开始时间
    private var gameStartTime: Long = 0

    // 游戏结束时间
    private var gameEndTime: Long = 0

    // 已用诗句列表
    private val usedLines = mutableListOf<String>()

    // 计时器相关
    private val _timeRemaining = MutableLiveData(30)
    val timeRemaining: LiveData<Int> = _timeRemaining

    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        startGame()
    }

    fun startGame() {
        resetGame()
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = ChainRequest(lastChar = "", usedLines = emptyList())
                val response = NetworkModule.poetryApiService.startChain(request)

                if (response.success) {
                    _currentChar.value = response.nextChar ?: ""
                    addMessage("系统", response.message)
                    // 提取起始诗句
                    val startLine = response.message.substringAfter("「").substringBefore("」")
                    usedLines.add(startLine)
                    startTimer()
                } else {
                    addMessage("系统", "游戏启动失败: ${response.message}")
                }
            } catch (e: Exception) {
                addMessage("系统", "网络错误: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun resetGame() {
        usedLines.clear()
        _chatMessages.value = emptyList()
        _isPlayerTurn.value = true
        _gameOver.value = false
        _winner.value = null
        _roundCount.value = 0
        _currentChar.value = ""
        _timeRemaining.value = 30
        stopTimer()
        gameStartTime = System.currentTimeMillis()
    }

    suspend fun submitAnswer(userLine: String): Boolean {
        if (_gameOver.value == true) return false
        if (_isPlayerTurn.value != true) return false
        if (userLine.trim().isEmpty()) return false

        _isLoading.value = true
        stopTimer()

        return try {
            val request = ChainRequest(
                userLine = userLine,
                lastChar = _currentChar.value ?: "",
                usedLines = usedLines.toList()
            )

            val result = NetworkModule.poetryApiService.judgeChain(request)

            if (result.success) {
                // 正确
                usedLines.add(userLine)
                addMessage("我", userLine)
                _roundCount.value = (_roundCount.value ?: 0) + 1
                _currentChar.value = result.nextChar ?: ""

                // AI回合
                val aiResult = aiTurn()

                if (aiResult == "认输") {
                    gameEnd("我")
                } else {
                    startTimer()
                }
                true
            } else {
                addMessage("系统", result.message)
                // 失败也开始新的计时器，给用户机会重新尝试
                startTimer()
                _isLoading.value = false
                false
            }
        } catch (e: Exception) {
            addMessage("系统", "网络错误: ${e.message}")
            startTimer()
            _isLoading.value = false
            false
        }
    }

    private suspend fun aiTurn(): String {
        _isPlayerTurn.value = false
        addMessage("AI", "思考中...")

        return try {
            val request = ChainRequest(
                lastChar = _currentChar.value ?: "",
                usedLines = usedLines.toList()
            )

            val response = NetworkModule.poetryApiService.chainAiTurn(request)

            if (!response.success || response.aiLine.isNullOrEmpty()) {
                // AI认输
                addMessage("AI", "我想不出来了... 认输！")
                "认输"
            } else {
                usedLines.add(response.aiLine)
                // 替换最后一条"思考中..."消息
                val currentMessages = _chatMessages.value?.toMutableList() ?: mutableListOf()
                if (currentMessages.isNotEmpty() && currentMessages.last().message == "思考中...") {
                    currentMessages.removeAt(currentMessages.size - 1)
                }
                currentMessages.add(ChatMessage("AI", response.aiLine))
                _chatMessages.value = currentMessages

                _currentChar.value = response.nextChar ?: ""
                _roundCount.value = (_roundCount.value ?: 0) + 1
                _isPlayerTurn.value = true
                response.aiLine
            }
        } catch (e: Exception) {
            addMessage("系统", "AI出错: ${e.message}")
            _isPlayerTurn.value = true
            ""
        }
    }

    private fun gameEnd(winner: String) {
        _gameOver.value = true
        _winner.value = winner
        _isPlayerTurn.value = false
        gameEndTime = System.currentTimeMillis()
        stopTimer()

        val duration = ((gameEndTime - gameStartTime) / 1000).toString()
        addMessage("系统", "🏆 游戏结束！${winner} 获胜！🏆")
        addMessage("系统", "📊 成绩统计：")
        addMessage("系统", "   接龙轮次：${_roundCount.value} 轮")
        addMessage("系统", "   用时：${duration} 秒")
    }

    private fun addMessage(sender: String, message: String) {
        val current = _chatMessages.value?.toMutableList() ?: mutableListOf()
        current.add(ChatMessage(sender, message))
        _chatMessages.value = current
    }

    private fun startTimer() {
        stopTimer()
        _timeRemaining.value = 30

        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (_timeRemaining.value ?: 0 > 0) {
                delay(1000)
                _timeRemaining.postValue((_timeRemaining.value ?: 0) - 1)
            }

            // 时间到
            if (_isPlayerTurn.value == true && !_gameOver.value!!) {
                withContext(Dispatchers.Main) {
                    timeOut()
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun timeOut() {
        stopTimer()
        addMessage("系统", "⏰ 时间到！")
        gameEnd("AI")
    }

    fun getGameDuration(): Long {
        return if (gameEndTime > 0) {
            (gameEndTime - gameStartTime) / 1000
        } else {
            (System.currentTimeMillis() - gameStartTime) / 1000
        }
    }

    data class ChatMessage(val sender: String, val message: String)
}
