package com.example.poetry.features.learning

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.poetry.features.learning.viewmodel.ChainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChainViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ChainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ChainViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 初始化测试 ====================

    @Test
    fun init_初始状态正确() {
        assertEquals("", viewModel.currentChar.value)
        assertTrue(viewModel.chatMessages.value?.isEmpty() ?: true)
        assertTrue(viewModel.isPlayerTurn.value == true)
        assertFalse(viewModel.isLoading.value == true)
        assertFalse(viewModel.gameOver.value == true)
        assertNull(viewModel.winner.value)
        assertEquals(0, viewModel.roundCount.value)
        assertEquals(30, viewModel.timeRemaining.value)
    }

    // ==================== 游戏状态测试 ====================

    @Test
    fun submitAnswer_gameOver时_返回false() = runTest {
        // 设置游戏结束状态
        setPrivateGameOver(viewModel, true)

        val result = viewModel.submitAnswer("床前明月光")

        assertFalse(result)
    }

    @Test
    fun submitAnswer_不是玩家回合_返回false() = runTest {
        // 设置非玩家回合
        setPrivatePlayerTurn(viewModel, false)

        val result = viewModel.submitAnswer("床前明月光")

        assertFalse(result)
    }

    @Test
    fun submitAnswer_空输入_返回false() = runTest {
        val result = viewModel.submitAnswer("")
        assertFalse(result)

        val result2 = viewModel.submitAnswer("   ")
        assertFalse(result2)
    }

    // ==================== 游戏结束测试 ====================

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
        assertNotNull(messages)
        assertFalse(messages!!.isEmpty())

        // 验证最后几条消息
        val lastMessage = messages.last()
        assertEquals("系统", lastMessage.sender)
        assertTrue(lastMessage.message.contains("游戏结束"))
        assertTrue(lastMessage.message.contains("AI"))
    }

    @Test
    fun gameEnd_统计信息正确() {
        callPrivateGameEnd(viewModel, "我")

        val messages = viewModel.chatMessages.value
        assertNotNull(messages)
        
        // 检查是否包含统计信息
        val statMessages = messages!!.filter { it.sender == "系统" && it.message.contains("成绩统计") }
        assertFalse(statMessages.isEmpty())
    }

    // ==================== 计时器测试 ====================

    @Test
    fun startTimer_初始化倒计时() {
        // 通过反射调用私有方法
        callPrivateStartTimer(viewModel)
        
        assertEquals(30, viewModel.timeRemaining.value)
    }

    @Test
    fun stopTimer_停止倒计时() {
        callPrivateStartTimer(viewModel)
        callPrivateStopTimer(viewModel)
        
        // 停止后时间应该保持不变
        val timeBefore = viewModel.timeRemaining.value
        // 短暂延迟后检查
        Thread.sleep(100)
        assertEquals(timeBefore, viewModel.timeRemaining.value)
    }

    // ==================== 消息管理测试 ====================

    @Test
    fun addMessage_添加消息到列表() {
        callPrivateAddMessage(viewModel, "我", "测试消息")

        val messages = viewModel.chatMessages.value
        assertNotNull(messages)
        assertEquals(1, messages!!.size)
        assertEquals("我", messages[0].sender)
        assertEquals("测试消息", messages[0].message)
    }

    @Test
    fun addMessage_多条消息_顺序正确() {
        callPrivateAddMessage(viewModel, "我", "消息1")
        callPrivateAddMessage(viewModel, "AI", "消息2")
        callPrivateAddMessage(viewModel, "系统", "消息3")

        val messages = viewModel.chatMessages.value
        assertNotNull(messages)
        assertEquals(3, messages!!.size)
        assertEquals("我", messages[0].sender)
        assertEquals("AI", messages[1].sender)
        assertEquals("系统", messages[2].sender)
    }

    // ==================== 轮次计数测试 ====================

    @Test
    fun roundCount_初始为0() {
        assertEquals(0, viewModel.roundCount.value)
    }

    // ==================== 辅助方法（反射） ====================

    private fun callPrivateAddMessage(viewModel: ChainViewModel, sender: String, message: String) {
        val method = ChainViewModel::class.java.getDeclaredMethod("addMessage", String::class.java, String::class.java)
        method.isAccessible = true
        method.invoke(viewModel, sender, message)
    }

    private fun callPrivateGameEnd(viewModel: ChainViewModel, winner: String) {
        val method = ChainViewModel::class.java.getDeclaredMethod("gameEnd", String::class.java)
        method.isAccessible = true
        method.invoke(viewModel, winner)
    }

    private fun callPrivateStartTimer(viewModel: ChainViewModel) {
        val method = ChainViewModel::class.java.getDeclaredMethod("startTimer")
        method.isAccessible = true
        method.invoke(viewModel)
    }

    private fun callPrivateStopTimer(viewModel: ChainViewModel) {
        val method = ChainViewModel::class.java.getDeclaredMethod("stopTimer")
        method.isAccessible = true
        method.invoke(viewModel)
    }

    private fun setPrivateGameOver(viewModel: ChainViewModel, value: Boolean) {
        val field = ChainViewModel::class.java.getDeclaredField("_gameOver")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val liveData = field.get(viewModel) as androidx.lifecycle.MutableLiveData<Boolean>
        liveData.value = value
    }

    private fun setPrivatePlayerTurn(viewModel: ChainViewModel, value: Boolean) {
        val field = ChainViewModel::class.java.getDeclaredField("_isPlayerTurn")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val liveData = field.get(viewModel) as androidx.lifecycle.MutableLiveData<Boolean>
        liveData.value = value
    }
}
