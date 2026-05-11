package com.example.poetry.features.ai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poetry.core.network.ApiChatRequest
import com.example.poetry.core.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.UUID

class AiChatViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var conversationId: String? = null

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
                val request = ApiChatRequest(
                    message = content,
                    conversationId = conversationId
                )

                val response = RetrofitClient.apiService.sendChatMessage(request).execute()

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    conversationId = apiResponse.conversationId

                    val aiMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = apiResponse.message,
                        isUser = false
                    )

                    val updatedMessages = _messages.value.orEmpty().toMutableList()
                    updatedMessages.add(aiMessage)
                    _messages.value = updatedMessages
                } else {
                    val errorMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = "抱歉，服务器返回错误：${response.code()}",
                        isUser = false
                    )
                    val updatedMessages = _messages.value.orEmpty().toMutableList()
                    updatedMessages.add(errorMessage)
                    _messages.value = updatedMessages
                }
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
