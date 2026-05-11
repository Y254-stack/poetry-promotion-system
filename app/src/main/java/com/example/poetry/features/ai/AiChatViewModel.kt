package com.example.poetry.features.ai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.UUID

class AiChatViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun sendMessage(content: String) {
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            isUser = true
        )

        val currentMessages = _messages.value.orEmpty().toMutableList()
        currentMessages.add(userMessage)
        _messages.value = currentMessages

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // TODO: Call backend API to get AI response
                // For now, use a mock response
                val aiResponse = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = "这是一个模拟回复。后续将接入 Claude API。",
                    isUser = false
                )

                val updatedMessages = _messages.value.orEmpty().toMutableList()
                updatedMessages.add(aiResponse)
                _messages.value = updatedMessages
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = "抱歉，发生了错误：${e.message}",
                    isUser = false
                )
                val updatedMessages = _messages.value.orEmpty().toMutableList()
                updatedMessages.add(errorMessage)
                _messages.value = updatedMessages
            } finally {
                _isLoading.value = false
            }
        }
    }
}
