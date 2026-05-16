package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.network.ApiFillBlankQuizDto
import com.example.poetry.core.network.ApiQuizSubmitRequest
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.databinding.FragmentFillBlankBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class FillBlankFragment : Fragment(R.layout.fragment_fill_blank) {

    private var _binding: FragmentFillBlankBinding? = null
    private val binding get() = _binding!!

    private var currentQuiz: ApiFillBlankQuizDto? = null
    private var fullAnswer = ""
    private var currentTranslation: String? = null
    private val answerGrids = mutableListOf<TextView>()
    private val candidateViews = mutableListOf<TextView>()
    private var selectedGridIndex = 0
    private var startTime: Long = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFillBlankBinding.bind(view)

        resetUiState()
        loadNextQuiz()

        binding.hintButton.setOnClickListener { provideHint() }
        binding.btnReset.setOnClickListener { resetCurrentQuiz() }
        binding.btnShowAnswer.setOnClickListener { showAnswer() }
        binding.btnTranslation.setOnClickListener { showTranslationPopup() }
        binding.btnCloseTranslation.setOnClickListener { hideTranslationPopup() }
        binding.translationPopup.setOnClickListener { hideTranslationPopup() }
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun resetUiState() {
        binding.poemTitle.text = "正在同步题目..."
        binding.poemAuthor.text = ""
        binding.candidatesGrid.removeAllViews()
        binding.row1.removeAllViews()
        binding.row2.removeAllViews()
        answerGrids.clear()
        candidateViews.clear()
        selectedGridIndex = 0

        binding.hintButton.isEnabled = true
        binding.btnShowAnswer.isEnabled = true
        binding.btnReset.isEnabled = true
    }

    private fun loadNextQuiz() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    NetworkModule.poetryApiService.getRandomFillBlankQuiz().awaitResponse()
                }

                if (response.isSuccessful) {
                    val quiz = response.body()
                    if (quiz != null) {
                        currentQuiz = quiz
                        displayQuiz(quiz)
                    } else {
                        binding.poemTitle.text = "暂无练习题目"
                    }
                } else {
                    binding.poemTitle.text = "同步失败 (${response.code()})"
                }
            } catch (e: Exception) {
                Log.e("FillBlank", "Load quiz failed", e)
                binding.poemTitle.text = "网络连接异常"
            }
        }
    }

    private fun displayQuiz(quiz: ApiFillBlankQuizDto) {
        binding.poemTitle.text = quiz.title
        binding.poemAuthor.text = quiz.author
        currentTranslation = quiz.translation

        fullAnswer = quiz.targetSentence.replace(Regex("[，。？！；：、\\s]"), "")
        startTime = System.currentTimeMillis()

        binding.root.post {
            setupAnswerGrids(fullAnswer.length)
            binding.root.post {
                setupCandidates(quiz.candidateWords)
            }
        }

        binding.btnTranslation.isEnabled = true
        binding.btnTranslation.alpha = 1f
    }

    private fun setupAnswerGrids(length: Int) {
        answerGrids.clear()
        binding.row1.removeAllViews()
        binding.row2.removeAllViews()

        val actualLength = length.coerceAtLeast(5)
        val firstRowCount = when {
            actualLength <= 5 -> actualLength
            actualLength == 6 -> 3
            else -> ceil(actualLength / 2f).toInt()
        }
        val secondRowCount = actualLength - firstRowCount
        val maxRowCount = max(firstRowCount, secondRowCount)

        val screenWidth = resources.displayMetrics.widthPixels
        val horizontalGap = dp(10)
        val availableWidth = screenWidth - dp(88)
        val gridSize = min(
            dp(42),
            ((availableWidth - horizontalGap * (maxRowCount - 1)) / maxRowCount).coerceAtLeast(dp(30))
        )

        repeat(actualLength) { index ->
            val grid = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(gridSize, gridSize).apply {
                    marginStart = horizontalGap / 2
                    marginEnd = horizontalGap / 2
                }
                background = resources.getDrawable(R.drawable.bg_character_box, null)
                gravity = android.view.Gravity.CENTER
                textSize = if (gridSize >= dp(38)) 16f else 14f
                setTextColor(resources.getColor(R.color.text_primary, null))
            }
            answerGrids += grid

            val container = if (index < firstRowCount) binding.row1 else binding.row2
            container.addView(grid)

            grid.setOnClickListener {
                if (grid.text.isNotEmpty()) {
                    findAndEnableCandidate(grid.text.toString())
                    grid.text = ""
                }
                updateSelection(index)
            }
        }

        updateSelection(0)
    }

    private fun setupCandidates(words: List<String>) {
        binding.candidatesGrid.removeAllViews()
        candidateViews.clear()
        if (words.isEmpty()) return

        val count = words.size
        val columns = when {
            count <= 4 -> count
            count <= 6 -> 3
            else -> 4
        }
        binding.candidatesGrid.columnCount = columns

        val horizontalGap = dp(10)
        val verticalGap = dp(10)
        val availableWidth = (binding.root.width - dp(64)).coerceAtLeast(dp(160))
        val availableHeight = (
            binding.bottomActionCard.top - binding.answerGrid.bottom - dp(26)
            ).coerceAtLeast(dp(56))
        val rows = ceil(count / columns.toFloat()).toInt().coerceAtLeast(1)
        val sizeByWidth =
            ((availableWidth - horizontalGap * (columns - 1)) / columns).coerceAtLeast(dp(30))
        val sizeByHeight =
            ((availableHeight - verticalGap * (rows - 1)) / rows).coerceAtLeast(dp(30))
        val cellSize = min(min(sizeByWidth, sizeByHeight), dp(46))
        val textSize = when {
            cellSize >= dp(42) -> 18f
            cellSize >= dp(36) -> 16f
            else -> 14f
        }

        words.forEach { word ->
            val textView = TextView(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cellSize
                    height = cellSize
                    marginStart = horizontalGap / 2
                    marginEnd = horizontalGap / 2
                    topMargin = verticalGap / 2
                    bottomMargin = verticalGap / 2
                }
                background = resources.getDrawable(R.drawable.bg_option_box, null)
                gravity = android.view.Gravity.CENTER
                text = word
                this.textSize = textSize
                setTextColor(resources.getColor(R.color.text_primary, null))
                isEnabled = true
            }
            textView.setOnClickListener {
                if (it.isEnabled) {
                    fillGrid(word, it)
                }
            }
            binding.candidatesGrid.addView(textView)
            candidateViews += textView
        }
    }

    private fun fillGrid(word: String, candidateView: View) {
        if (selectedGridIndex !in answerGrids.indices) return
        val grid = answerGrids[selectedGridIndex]
        if (grid.text.isNotEmpty()) {
            findAndEnableCandidate(grid.text.toString())
        }
        grid.text = word
        candidateView.isEnabled = false
        candidateView.alpha = 0.45f

        val nextEmpty = answerGrids.indexOfFirst { it.text.isEmpty() }
        if (nextEmpty != -1) {
            updateSelection(nextEmpty)
        } else {
            checkAnswer()
        }
    }

    private fun updateSelection(index: Int) {
        answerGrids.forEach { it.isSelected = false }
        if (index in answerGrids.indices) {
            selectedGridIndex = index
            answerGrids[index].isSelected = true
        }
    }

    private fun findAndEnableCandidate(word: String) {
        candidateViews.forEach { view ->
            if (view.text == word && !view.isEnabled) {
                view.isEnabled = true
                view.alpha = 1f
            }
        }
    }

    private fun provideHint() {
        val emptyIndex = answerGrids.indexOfFirst { it.text.isEmpty() }
        if (emptyIndex == -1) {
            Toast.makeText(context, "已经没有空格了", Toast.LENGTH_SHORT).show()
            return
        }
        val correctChar = fullAnswer[emptyIndex].toString()
        updateSelection(emptyIndex)
        Toast.makeText(
            context,
            "提示：第 ${emptyIndex + 1} 个字是「$correctChar」",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showTranslationPopup() {
        if (!currentTranslation.isNullOrEmpty()) {
            binding.translationContent.text = currentTranslation
            binding.translationPopup.visibility = View.VISIBLE
        } else {
            Toast.makeText(context, "暂无译文", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideTranslationPopup() {
        binding.translationPopup.visibility = View.GONE
    }

    private fun showAnswer() {
        fullAnswer.forEachIndexed { index, char ->
            if (index !in answerGrids.indices) return@forEachIndexed
            answerGrids[index].text = char.toString()
            answerGrids[index].background =
                resources.getDrawable(R.drawable.bg_character_box_correct, null)
            answerGrids[index].setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        }

        candidateViews.forEach { view ->
            view.isEnabled = false
            view.alpha = 0.3f
        }

        binding.hintButton.isEnabled = false
        binding.btnShowAnswer.isEnabled = false
        binding.btnReset.isEnabled = false

        Toast.makeText(context, "已显示正确答案", Toast.LENGTH_LONG).show()

        lifecycleScope.launch {
            delay(1800)
            resetUiState()
            loadNextQuiz()
        }
    }

    private fun checkAnswer() {
        if (answerGrids.any { it.text.isEmpty() }) return

        val userAnswer = answerGrids.joinToString("") { it.text.toString() }
        if (userAnswer.length != fullAnswer.length) return

        val isCorrect = userAnswer == fullAnswer
        submitResult(isCorrect)
        if (isCorrect) {
            Toast.makeText(context, "答对了，太棒了", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                delay(1500)
                resetUiState()
                loadNextQuiz()
            }
        } else {
            Toast.makeText(context, "答案有误，再检查一下吧", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetCurrentQuiz() {
        answerGrids.forEach { grid ->
            if (grid.text.isNotEmpty()) {
                findAndEnableCandidate(grid.text.toString())
                grid.text = ""
            }
            grid.background = resources.getDrawable(R.drawable.bg_character_box, null)
            grid.setTextColor(resources.getColor(R.color.text_primary, null))
        }
        binding.hintButton.isEnabled = true
        binding.btnShowAnswer.isEnabled = true
        binding.btnReset.isEnabled = true
        updateSelection(0)
    }

    private fun submitResult(isCorrect: Boolean) {
        val duration = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        val quiz = currentQuiz ?: return
        val userId = SessionManager(requireContext()).userId()
        if (userId <= 0L) {
            Log.w("FillBlank", "Skip quiz sync: user not logged in")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = ApiQuizSubmitRequest(
                    userId = userId,
                    workId = quiz.workId,
                    sentenceId = quiz.sentenceId,
                    quizType = "fill_blank",
                    isCorrect = isCorrect,
                    durationSeconds = duration,
                    questionPayload = "{}",
                    answerPayload = "{}",
                    correctPayload = "{}"
                )
                NetworkModule.poetryApiService.submitQuizResult(request).awaitResponse()
            } catch (e: Exception) {
                Log.e("FillBlank", "Sync quiz result failed", e)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
