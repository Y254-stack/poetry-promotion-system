package com.example.poetry.features.learning

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.poetry.features.learning.model.QuizQuestion
import com.example.poetry.features.learning.viewmodel.QuizViewModel
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class QuizViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: QuizViewModel

    @Before
    fun setUp() {
        viewModel = QuizViewModel()
        // 清空内部题目列表，用测试数据替代
        setPrivateQuestions(viewModel, createTestQuestions())
    }

    @After
    fun tearDown() {
        // 清理
    }

    // ==================== 答案比较逻辑测试 ====================

    @Test
    fun submitAnswer_正确答案_正确计数增加() {
        // 设置第一题正确答案为"疑是地上霜"
        val questions = listOf(
            QuizQuestion(1, "床前明月光，", "疑是地上霜", "静夜思", "李白")
        )
        setPrivateQuestions(viewModel, questions)

        viewModel.submitAnswer("疑是地上霜")

        assertEquals(1, viewModel.correctCount.value)
        assertEquals(0, viewModel.wrongCount.value)
    }

    @Test
    fun submitAnswer_错误答案_错误计数增加() {
        val questions = listOf(
            QuizQuestion(1, "床前明月光，", "疑是地上霜", "静夜思", "李白")
        )
        setPrivateQuestions(viewModel, questions)

        viewModel.submitAnswer("错误答案")

        assertEquals(0, viewModel.correctCount.value)
        assertEquals(1, viewModel.wrongCount.value)
    }

    @Test
    fun submitAnswer_忽略标点符号_正确匹配() {
        val questions = listOf(
            QuizQuestion(1, "床前明月光，", "疑是地上霜", "静夜思", "李白")
        )
        setPrivateQuestions(viewModel, questions)

        viewModel.submitAnswer("疑是地上霜。")

        assertEquals(1, viewModel.correctCount.value)
    }

    @Test
    fun submitAnswer_忽略句号分号_正确匹配() {
        val questions = listOf(
            QuizQuestion(1, "床前明月光，", "疑是地上霜", "静夜思", "李白")
        )
        setPrivateQuestions(viewModel, questions)

        viewModel.submitAnswer("疑是地上霜；")

        assertEquals(1, viewModel.correctCount.value)
    }

    @Test
    fun submitAnswer_忽略空格_正确匹配() {
        val questions = listOf(
            QuizQuestion(1, "床前明月光，", "疑是地上霜", "静夜思", "李白")
        )
        setPrivateQuestions(viewModel, questions)

        viewModel.submitAnswer("疑是 地上 霜")

        assertEquals(1, viewModel.correctCount.value)
    }

    @Test
    fun submitAnswer_忽略大小写_正确匹配() {
        val questions = listOf(
            QuizQuestion(1, "abc", "ABC", "测试", "测试")
        )
        setPrivateQuestions(viewModel, questions)

        viewModel.submitAnswer("abc")

        assertEquals(1, viewModel.correctCount.value)
    }

    // ==================== 进度和导航测试 ====================

    @Test
    fun submitAnswer_最后一题_完成后isFinished为true() {
        val questions = listOf(
            QuizQuestion(1, "第一题", "答案1", "", ""),
            QuizQuestion(2, "第二题", "答案2", "", "")
        )
        setPrivateQuestions(viewModel, questions)

        // 答完第一题
        viewModel.submitAnswer("答案1")
        assertFalse(viewModel.isFinished.value!!)
        assertEquals("答案2", viewModel.currentQuestion.value?.correctAnswer)

        // 答完第二题
        viewModel.submitAnswer("答案2")
        assertTrue(viewModel.isFinished.value!!)
        assertNull(viewModel.currentQuestion.value)
    }

    @Test
    fun submitAnswer_答对后_进度增加() {
        val questions = listOf(
            QuizQuestion(1, "第一题", "答案1", "", ""),
            QuizQuestion(2, "第二题", "答案2", "", ""),
            QuizQuestion(3, "第三题", "答案3", "", "")
        )
        setPrivateQuestions(viewModel, questions)

        viewModel.submitAnswer("答案1")
        assertEquals(2, viewModel.progress.value)

        viewModel.submitAnswer("答案2")
        assertEquals(3, viewModel.progress.value)
    }

    // ==================== 重新开始测试 ====================

    @Test
    fun restartQuiz_重置所有状态() {
        val questions = listOf(
            QuizQuestion(1, "第一题", "答案1", "", ""),
            QuizQuestion(2, "第二题", "答案2", "", "")
        )
        setPrivateQuestions(viewModel, questions)

        // 先答几题
        viewModel.submitAnswer("答案1")
        viewModel.submitAnswer("答案2")

        // 重置
        viewModel.restartQuiz()

        assertEquals(0, viewModel.correctCount.value)
        assertEquals(0, viewModel.wrongCount.value)
        assertEquals(0, viewModel.elapsedSeconds.value)
        assertFalse(viewModel.isFinished.value!!)
        assertEquals(1, viewModel.progress.value)
        assertNotNull(viewModel.currentQuestion.value)
    }

    // ==================== 计时测试 ====================

    @Test
    fun tick_未完成_时间增加() {
        val questions = listOf(
            QuizQuestion(1, "第一题", "答案1", "", "")
        )
        setPrivateQuestions(viewModel, questions)

        val initialTime = viewModel.elapsedSeconds.value ?: 0
        viewModel.tick()
        assertEquals(initialTime + 1, viewModel.elapsedSeconds.value)
    }

    @Test
    fun tick_已完成_时间不增加() {
        val questions = listOf(
            QuizQuestion(1, "第一题", "答案1", "", "")
        )
        setPrivateQuestions(viewModel, questions)

        viewModel.submitAnswer("答案1")
        assertTrue(viewModel.isFinished.value!!)

        val timeBeforeTick = viewModel.elapsedSeconds.value ?: 0
        viewModel.tick()
        assertEquals(timeBeforeTick, viewModel.elapsedSeconds.value)
    }

    // ==================== Toast消息测试 ====================

    @Test
    fun submitAnswer_正确答案_toast消息正确() {
        val questions = listOf(
            QuizQuestion(1, "床前明月光", "疑是地上霜", "", "")
        )
        setPrivateQuestions(viewModel, questions)

        viewModel.submitAnswer("疑是地上霜")

        assertEquals("✅ 回答正确！", viewModel.toastMessage.value)
    }

    @Test
    fun submitAnswer_错误答案_toast显示正确答案() {
        val questions = listOf(
            QuizQuestion(1, "床前明月光", "疑是地上霜", "", "")
        )
        setPrivateQuestions(viewModel, questions)

        viewModel.submitAnswer("错误答案")

        assertEquals("❌ 回答错误！正确答案是：疑是地上霜", viewModel.toastMessage.value)
    }

    @Test
    fun toastMessageShown_清除消息() {
        val questions = listOf(
            QuizQuestion(1, "床前明月光", "疑是地上霜", "", "")
        )
        setPrivateQuestions(viewModel, questions)

        viewModel.submitAnswer("疑是地上霜")
        assertEquals("✅ 回答正确！", viewModel.toastMessage.value)

        viewModel.toastMessageShown()
        assertNull(viewModel.toastMessage.value)
    }

    // ==================== 辅助方法 ====================

    /**
     * 通过反射设置 ViewModel 内部的题目列表
     */
    private fun setPrivateQuestions(viewModel: QuizViewModel, questions: List<QuizQuestion>) {
        val field = QuizViewModel::class.java.getDeclaredField("_questions")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val mutableList = field.get(viewModel) as MutableList<QuizQuestion>
        mutableList.clear()
        mutableList.addAll(questions)

        // 重置 currentQuestion
        val currentQuestionField = QuizViewModel::class.java.getDeclaredField("_currentQuestion")
        currentQuestionField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val currentQuestionLiveData = currentQuestionField.get(viewModel) as androidx.lifecycle.MutableLiveData<QuizQuestion?>
        currentQuestionLiveData.value = questions.firstOrNull()

        // 重置 progress
        val progressField = QuizViewModel::class.java.getDeclaredField("_progress")
        progressField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val progressLiveData = progressField.get(viewModel) as androidx.lifecycle.MutableLiveData<Int>
        progressLiveData.value = 1

        // 重置 isFinished
        val finishedField = QuizViewModel::class.java.getDeclaredField("_isFinished")
        finishedField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val finishedLiveData = finishedField.get(viewModel) as androidx.lifecycle.MutableLiveData<Boolean>
        finishedLiveData.value = false

        // 重置正确错误计数
        val correctField = QuizViewModel::class.java.getDeclaredField("_correctCount")
        correctField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val correctLiveData = correctField.get(viewModel) as androidx.lifecycle.MutableLiveData<Int>
        correctLiveData.value = 0

        val wrongField = QuizViewModel::class.java.getDeclaredField("_wrongCount")
        wrongField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val wrongLiveData = wrongField.get(viewModel) as androidx.lifecycle.MutableLiveData<Int>
        wrongLiveData.value = 0
    }

    private fun createTestQuestions(): List<QuizQuestion> {
        return listOf(
            QuizQuestion(1, "床前明月光，", "疑是地上霜", "静夜思", "李白"),
            QuizQuestion(2, "白日依山尽，", "黄河入海流", "登鹳雀楼", "王之涣"),
            QuizQuestion(3, "春眠不觉晓，", "处处闻啼鸟", "春晓", "孟浩然")
        )
    }
}