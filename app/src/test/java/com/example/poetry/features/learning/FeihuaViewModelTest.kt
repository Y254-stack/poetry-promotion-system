package com.example.poetry.features.learning

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.poetry.features.learning.viewmodel.FeihuaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeihuaViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: FeihuaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FeihuaViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 初始化和重置测试 ====================

    @Test
    fun init_随机生成关键字() {
        val keyword = viewModel.keyword.value
        assertNotNull(keyword)
        assertTrue(keyword!!.isNotEmpty())
    }

    @Test
    fun resetGame_清空所有状态() {
        // 先添加一些数据
        addPrivateMessage(viewModel, "我", "测试消息")
        setPrivatePlayerTurn(viewModel, false)
        setPrivateGameOver(viewModel, true)
        setPrivateWinner(viewModel, "AI")

        // 重置
        viewModel.resetGame()

        assertTrue(viewModel.chatMessages.value?.isEmpty() ?: true)
        assertTrue(viewModel.isPlayerTurn.value == true)
        assertTrue(viewModel.gameOver.value == false)
        assertNull(viewModel.winner.value)
    }

    @Test
    fun randomKeyword_重置游戏_清空消息列表() {
        // 先添加一些数据
        addPrivateMessage(viewModel, "我", "测试消息")
        addPrivateMessage(viewModel, "AI", "回复消息")

        val oldKeyword = viewModel.keyword.value
        viewModel.randomKeyword()

        // 关键字应该改变（或者可能相同，但概率低）
        // 消息列表应该被清空
        assertTrue(viewModel.chatMessages.value?.isEmpty() ?: true)
    }

    @Test
    fun randomKeyword_添加系统消息() {
        viewModel.randomKeyword()

        val messages = viewModel.chatMessages.value
        assertNotNull(messages)
        assertTrue(messages!!.isNotEmpty())
        assertEquals("系统", messages.first().sender)
        assertTrue(messages.first().message.contains("新的一局"))
    }

    @Test
    fun changeKeyword_调用randomKeyword() {
        // changeKeyword 内部调用 randomKeyword
        val oldKeyword = viewModel.keyword.value
        viewModel.changeKeyword()

        // 由于是随机，可能相同，不强制验证不同
        // 验证不会崩溃即可
        assertNotNull(viewModel.keyword.value)
    }

    // ==================== 游戏状态测试 ====================

    @Test
    fun submitAnswer_gameOver时_返回false() = runBlockingTest {
        setPrivateGameOver(viewModel, true)

        val result = viewModel.submitAnswer("床前明月光")

        assertFalse(result)
    }

    @Test
    fun submitAnswer_不是玩家回合_返回false() = runBlockingTest {
        setPrivatePlayerTurn(viewModel, false)

        val result = viewModel.submitAnswer("床前明月光")

        assertFalse(result)
    }

    // ==================== 胜负判断测试 ====================

    @Test
    fun gameEnd_设置游戏结束状态() {
        callPrivateGameEnd(viewModel, "我")

        assertTrue(viewModel.gameOver.value == true)
        assertEquals("我", viewModel.winner.value)
        assertTrue(viewModel.isPlayerTurn.value == false)
    }

    @Test
    fun gameEnd_添加系统消息() {
        callPrivateGameEnd(viewModel, "AI")

        val messages = viewModel.chatMessages.value
        val lastMessage = messages?.lastOrNull()
        assertNotNull(lastMessage)
        assertEquals("系统", lastMessage?.sender)
        assertTrue(lastMessage?.message?.contains("AI 获胜") == true)
    }

    // ==================== 辅助方法（反射） ====================

    private fun addPrivateMessage(viewModel: FeihuaViewModel, sender: String, message: String) {
        val method = FeihuaViewModel::class.java.getDeclaredMethod("addMessage", String::class.java, String::class.java)
        method.isAccessible = true
        method.invoke(viewModel, sender, message)
    }

    private fun setPrivatePlayerTurn(viewModel: FeihuaViewModel, value: Boolean) {
        val field = FeihuaViewModel::class.java.getDeclaredField("_isPlayerTurn")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val liveData = field.get(viewModel) as androidx.lifecycle.MutableLiveData<Boolean>
        liveData.value = value
    }

    private fun setPrivateGameOver(viewModel: FeihuaViewModel, value: Boolean) {
        val field = FeihuaViewModel::class.java.getDeclaredField("_gameOver")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val liveData = field.get(viewModel) as androidx.lifecycle.MutableLiveData<Boolean>
        liveData.value = value
    }

    private fun setPrivateWinner(viewModel: FeihuaViewModel, value: String?) {
        val field = FeihuaViewModel::class.java.getDeclaredField("_winner")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val liveData = field.get(viewModel) as androidx.lifecycle.MutableLiveData<String?>
        liveData.value = value
    }

    private fun callPrivateGameEnd(viewModel: FeihuaViewModel, winner: String) {
        val method = FeihuaViewModel::class.java.getDeclaredMethod("gameEnd", String::class.java)
        method.isAccessible = true
        method.invoke(viewModel, winner)
    }

    // 简单的 runBlocking 扩展
    private fun runBlockingTest(block: suspend () -> Unit) {
        kotlinx.coroutines.test.runTest {
            block()
        }
    }
}