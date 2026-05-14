package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class FillBlankFragment : Fragment(R.layout.fragment_fill_blank) {

    private var _binding: FragmentFillBlankBinding? = null
    private val binding get() = _binding!!

    private var currentQuiz: ApiFillBlankQuizDto? = null
    private var fullAnswer = ""
    private var currentTranslation: String? = null
    private val answerGrids = mutableListOf<TextView>()
    private var selectedGridIndex = 0
    private var startTime: Long = 0
    private val candidateViews = mutableListOf<TextView>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFillBlankBinding.bind(view)

        resetUiState()
        loadNextQuiz()
        
        binding.hintButton.setOnClickListener {
            provideHint()
        }
        
        binding.btnReset.setOnClickListener {
            resetCurrentQuiz()
        }
        
        binding.btnShowAnswer.setOnClickListener {
            showAnswer()
        }
        
        binding.btnTranslation.setOnClickListener {
            showTranslationPopup()
        }
        
        binding.btnCloseTranslation.setOnClickListener {
            hideTranslationPopup()
        }
        
        binding.translationPopup.setOnClickListener {
            hideTranslationPopup()
        }
        
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun resetUiState() {
        binding.poemTitle.text = "正在同步后台数据..."
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
                    val code = response.code()
                    Log.e("FillBlank", "API Error: $code")
                    binding.poemTitle.text = "同步失败 ($code)"
                }
            } catch (e: Exception) {
                Log.e("FillBlank", "Connection Fail", e)
                binding.poemTitle.text = "网络连接异常"
            }
        }
    }

    private fun displayQuiz(quiz: ApiFillBlankQuizDto) {
        binding.poemTitle.text = quiz.title
        binding.poemAuthor.text = quiz.author
        currentTranslation = quiz.translation
        
        fullAnswer = quiz.targetSentence.replace(Regex("[，。？！；：、]"), "")
        startTime = System.currentTimeMillis()
        setupGrids(fullAnswer.length)
        setupCandidates(quiz.candidateWords)
        
        binding.btnTranslation.isEnabled = true
        binding.btnTranslation.alpha = 1.0f
    }

    private fun setupGrids(length: Int) {
        answerGrids.clear()
        binding.row1.removeAllViews()
        binding.row2.removeAllViews()
        
        val actualLength = if (length > 0) length else 5
        Log.d("FillBlank", "Setting up $actualLength grids")
        
        val firstRowSize = if (actualLength > 6) actualLength / 2 else actualLength
        for (i in 0 until actualLength) {
            val grid = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(105, 105).apply {
                    marginStart = 10
                    marginEnd = 10
                }
                background = resources.getDrawable(R.drawable.bg_character_box, null)
                gravity = android.view.Gravity.CENTER
                textSize = 28f
                setTextColor(resources.getColor(R.color.text_primary, null))
            }
            answerGrids.add(grid)
            val container = if (i < firstRowSize) binding.row1 else binding.row2
            container.addView(grid)
            val index = i
            grid.setOnClickListener {
                if (grid.text.isNotEmpty()) {
                    findAndEnableCandidate(grid.text.toString())
                    grid.text = ""
                }
                updateSelection(index)
            }
        }
        Log.d("FillBlank", "Grids setup completed, row1 has ${binding.row1.childCount} views, row2 has ${binding.row2.childCount} views")
        updateSelection(0)
    }

    private fun setupCandidates(words: List<String>) {
        binding.candidatesGrid.removeAllViews()
        candidateViews.clear()
        
        words.forEach { word ->
            val textView = TextView(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 120
                    height = 120
                    marginStart = 24
                    marginEnd = 24
                    topMargin = 24
                    bottomMargin = 24
                }
                background = resources.getDrawable(R.drawable.bg_option_box, null)
                gravity = android.view.Gravity.CENTER
                text = word
                textSize = 32f
                setTextColor(resources.getColor(R.color.text_primary, null))
                isEnabled = true
            }
            textView.setOnClickListener {
                if (it.isEnabled) {
                    fillGrid(word, it)
                }
            }
            binding.candidatesGrid.addView(textView)
            candidateViews.add(textView)
        }
    }

    private fun fillGrid(word: String, candidateView: View) {
        if (selectedGridIndex < answerGrids.size) {
            val grid = answerGrids[selectedGridIndex]
            if (grid.text.isNotEmpty()) findAndEnableCandidate(grid.text.toString())
            grid.text = word
            candidateView.isEnabled = false
            candidateView.alpha = 0.5f
            val nextEmpty = answerGrids.indexOfFirst { it.text.isEmpty() }
            if (nextEmpty != -1) updateSelection(nextEmpty) else checkAnswer()
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
                view.alpha = 1.0f
            }
        }
    }

    private fun provideHint() {
        val emptyIndex = answerGrids.indexOfFirst { it.text.isEmpty() }
        if (emptyIndex != -1) {
            val correctChar = fullAnswer[emptyIndex].toString()
            answerGrids[emptyIndex].isSelected = true
            selectedGridIndex = emptyIndex
            Toast.makeText(context, "提示：第${emptyIndex + 1}个字是「$correctChar」", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "已经没有空格子了！", Toast.LENGTH_SHORT).show()
        }
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
        for (i in fullAnswer.indices) {
            val correctChar = fullAnswer[i].toString()
            answerGrids[i].text = correctChar
            answerGrids[i].background = resources.getDrawable(R.drawable.bg_character_box_correct, null)
            answerGrids[i].setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
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
            delay(2000)
            resetUiState()
            loadNextQuiz()
        }
    }

    private fun checkAnswer() {
        val hasEmptyGrid = answerGrids.any { it.text.isEmpty() }
        if (hasEmptyGrid) {
            return
        }
        
        val userAnswer = answerGrids.joinToString("") { it.text.toString() }
        if (userAnswer.length == fullAnswer.length) {
            val isCorrect = userAnswer == fullAnswer
            submitResult(isCorrect)
            if (isCorrect) {
                Toast.makeText(context, "答对了！太棒了", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch {
                    delay(1500)
                    resetUiState()
                    loadNextQuiz()
                }
            } else {
                Toast.makeText(context, "答案有误，再检查一下吧", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetCurrentQuiz() {
        answerGrids.forEach { 
            if (it.text.isNotEmpty()) {
                findAndEnableCandidate(it.text.toString())
                it.text = ""
            }
        }
        updateSelection(0)
    }

    private fun submitResult(isCorrect: Boolean) {
        val duration = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        val quiz = currentQuiz ?: return
        val userId = SessionManager(requireContext()).userId()
        if (userId <= 0L) {
            Log.w("FillBlank", "Skip quiz record sync: user not logged in")
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
                Log.e("FillBlank", "Record Sync Fail", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
