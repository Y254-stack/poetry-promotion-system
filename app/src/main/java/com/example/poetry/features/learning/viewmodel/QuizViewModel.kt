package com.example.poetry.features.learning.viewmodel


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.features.learning.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizViewModel : ViewModel() {

    companion object {
        private const val TAG = "QuizViewModel"
    }

    private val _questions = mutableListOf<QuizQuestion>()
    val totalQuestions: Int get() = _questions.size

    private var currentIndex = 0

    private val _currentQuestion = MutableLiveData<QuizQuestion?>()
    val currentQuestion: LiveData<QuizQuestion?> = _currentQuestion

    private val _progress = MutableLiveData(1)
    val progress: LiveData<Int> = _progress

    private val _correctCount = MutableLiveData(0)
    val correctCount: LiveData<Int> = _correctCount

    private val _wrongCount = MutableLiveData(0)
    val wrongCount: LiveData<Int> = _wrongCount

    private val _elapsedSeconds = MutableLiveData(0)
    val elapsedSeconds: LiveData<Int> = _elapsedSeconds

    private val _isFinished = MutableLiveData(false)
    val isFinished: LiveData<Boolean> = _isFinished

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        Log.e(TAG, "========== QuizViewModel 初始化 ==========")
        loadQuestionsFromBackend()
    }

    private fun loadQuestionsFromBackend() {
        Log.e(TAG, "========== 开始调用后端接口 ==========")

        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.e(TAG, "发起网络请求...")

                val response = withContext(Dispatchers.IO) {
                    NetworkModule.poetryApiService.getQuizQuestions(20).execute()
                }

                Log.e(TAG, "HTTP状态码: ${response.code()}")
                Log.e(TAG, "请求成功: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val quizList = response.body() ?: emptyList()
                    Log.e(TAG, "返回题目数量: ${quizList.size}")

                    if (quizList.isNotEmpty()) {
                        _questions.clear()
                        _questions.addAll(quizList.map { dto ->
                            QuizQuestion(
                                id = dto.id,
                                firstLine = dto.firstLine ?: "",
                                correctAnswer = dto.correctAnswer ?: "",
                                sourceTitle = dto.sourceTitle,
                                sourceAuthor = dto.sourceAuthor
                            )
                        })
                        _currentQuestion.value = _questions.firstOrNull()
                        _progress.value = 1
                        Log.e(TAG, "✅ 后端数据加载成功，第一题: ${_questions.firstOrNull()?.firstLine}")
                    } else {
                        Log.e(TAG, "⚠️ 后端返回空列表，使用硬编码")
                        loadMockQuestions()
                    }
                } else {
                    Log.e(TAG, "❌ HTTP错误 ${response.code()}: ${response.message()}")
                    loadMockQuestions()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 异常: ${e.javaClass.simpleName} - ${e.message}")
                e.printStackTrace()
                loadMockQuestions()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadMockQuestions() {
        _questions.clear()
        _questions.addAll(
            listOf(
                QuizQuestion(1, "床前明月光，", "疑是地上霜", "静夜思", "李白"),
                QuizQuestion(2, "白日依山尽，", "黄河入海流", "登鹳雀楼", "王之涣"),
                QuizQuestion(3, "春眠不觉晓，", "处处闻啼鸟", "春晓", "孟浩然"),
                QuizQuestion(4, "锄禾日当午，", "汗滴禾下土", "悯农", "李绅"),
                QuizQuestion(5, "离离原上草，", "一岁一枯荣", "赋得古原草送别", "白居易"),
                QuizQuestion(6, "向晚意不适，", "驱车登古原", "乐游原", "李商隐"),
                QuizQuestion(7, "千山鸟飞绝，", "万径人踪灭", "江雪", "柳宗元"),
                QuizQuestion(8, "空山不见人，", "但闻人语响", "鹿柴", "王维"),
                QuizQuestion(9, "红豆生南国，", "春来发几枝", "相思", "王维"),
                QuizQuestion(10, "好雨知时节，", "当春乃发生", "春夜喜雨", "杜甫"),
                QuizQuestion(11, "明月几时有，", "把酒问青天", "水调歌头", "苏轼"),
                QuizQuestion(12, "大漠沙如雪，", "燕山月似钩", "马诗", "李贺"),
                QuizQuestion(13, "采菊东篱下，", "悠然见南山", "饮酒", "陶渊明"),
                QuizQuestion(14, "海内存知己，", "天涯若比邻", "送杜少府之任蜀州", "王勃"),
                QuizQuestion(15, "烽火连三月，", "家书抵万金", "春望", "杜甫"),
                QuizQuestion(16, "举头望明月，", "低头思故乡", "静夜思", "李白"),
                QuizQuestion(17, "随风潜入夜，", "润物细无声", "春夜喜雨", "杜甫"),
                QuizQuestion(18, "野火烧不尽，", "春风吹又生", "赋得古原草送别", "白居易"),
                QuizQuestion(19, "欲穷千里目，", "更上一层楼", "登鹳雀楼", "王之涣"),
                QuizQuestion(20, "不敢高声语，", "恐惊天上人", "夜宿山寺", "李白")
            )
        )
        _currentQuestion.value = _questions.firstOrNull()
    }

    fun submitAnswer(userAnswer: String) {
        val current = _currentQuestion.value ?: return

        // 简单去标点、去空格比较
        val normalizedUser = userAnswer
            .replace(Regex("[，。？！；：、\\s]"), "")
            .trim()
            .lowercase()
        val normalizedCorrect = current.correctAnswer
            .replace(Regex("[，。？！；：、\\s]"), "")
            .trim()
            .lowercase()

        val isCorrect = normalizedUser == normalizedCorrect

        if (isCorrect) {
            _correctCount.value = (_correctCount.value ?: 0) + 1
            _toastMessage.value = "✅ 回答正确！"
        } else {
            _wrongCount.value = (_wrongCount.value ?: 0) + 1
            _toastMessage.value = "❌ 回答错误！正确答案是：${current.correctAnswer}"
        }

        moveToNextQuestion()
    }

    private fun moveToNextQuestion() {
        if (currentIndex + 1 < _questions.size) {
            currentIndex++
            _currentQuestion.value = _questions[currentIndex]
            _progress.value = currentIndex + 1
        } else {
            _isFinished.value = true
            _currentQuestion.value = null
        }
    }

    fun restartQuiz() {
        currentIndex = 0
        _correctCount.value = 0
        _wrongCount.value = 0
        _elapsedSeconds.value = 0
        _isFinished.value = false
        _currentQuestion.value = _questions.firstOrNull()
        _progress.value = 1
    }

    fun tick() {
        if (_isFinished.value != true) {
            _elapsedSeconds.value = (_elapsedSeconds.value ?: 0) + 1
        }
    }

    fun toastMessageShown() {
        _toastMessage.value = null
    }

    fun resetAndRestart() {
        currentIndex = 0
        _correctCount.value = 0
        _wrongCount.value = 0
        _elapsedSeconds.value = 0
        _isFinished.value = false
        _currentQuestion.value = _questions.firstOrNull()
        _progress.value = 1
    }
}