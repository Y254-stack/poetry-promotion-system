package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.poetry.R
import com.example.poetry.databinding.FragmentQuizBinding
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.poetry.features.learning.viewmodel.QuizViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import kotlin.time.Duration.Companion.seconds

class QuizFragment : Fragment(R.layout.fragment_quiz) {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: QuizViewModel
    private var timerJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuizBinding.bind(view)

        viewModel = ViewModelProvider(this)[QuizViewModel::class.java]

        setupListeners()
        observeViewModel()
        startTimer()
    }

    private fun setupListeners() {
        // 返回按钮
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 提交按钮
        binding.btnSubmit.setOnClickListener {
            val answer = binding.etAnswer.text.toString().trim()
            if (answer.isEmpty()) {
                Toast.makeText(requireContext(), "请输入下一句", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.submitAnswer(answer)
        }
    }

    private fun observeViewModel() {
        // 当前题目
        viewModel.currentQuestion.observe(viewLifecycleOwner) { question ->
            question?.let {
                binding.tvQuestionText.text = it.firstLine
                binding.etAnswer.setText("")
            }
        }

        // 进度
        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            binding.tvProgress.text = "第 $progress / ${viewModel.totalQuestions} 题"
        }

        // 正确数
        viewModel.correctCount.observe(viewLifecycleOwner) { correct ->
            binding.tvCorrect.text = "✅ $correct"
        }

        // 错误数
        viewModel.wrongCount.observe(viewLifecycleOwner) { wrong ->
            binding.tvWrong.text = "❌ $wrong"
        }

        // 计时
        viewModel.elapsedSeconds.observe(viewLifecycleOwner) { seconds ->
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            binding.tvTimer.text = String.format("%02d:%02d", minutes, remainingSeconds)
        }

        // Toast 消息
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.toastMessageShown()
            }
        }

        // 完成状态
        viewModel.isFinished.observe(viewLifecycleOwner) { finished ->
            if (finished) {
                stopTimer()
                showCompleteDialog()
            }
        }
    }

    private fun showCompleteDialog() {
        val correct = viewModel.correctCount.value ?: 0
        val wrong = viewModel.wrongCount.value ?: 0
        val total = viewModel.totalQuestions
        val accuracy = if (total > 0) (correct * 100 / total) else 0

        val seconds = viewModel.elapsedSeconds.value ?: 0

        val bundle = Bundle().apply {
            putInt("correctCount", correct)
            putInt("wrongCount", wrong)
            putInt("totalQuestions", total)
            putInt("elapsedSeconds", seconds)
        }

        findNavController().navigate(R.id.quizResultFragment, bundle)

        val message = """
            总题数：$total
            正确：$correct
            错误：$wrong
            正确率：${accuracy}%
        """.trimIndent()

        Toast.makeText(requireContext(), "🎉 测验完成！\n$message", Toast.LENGTH_LONG).show()

        // 可选：3秒后重新开始
        binding.btnSubmit.isEnabled = false
        binding.etAnswer.isEnabled = false
    }

    private fun startTimer() {
        stopTimer()
        timerJob = lifecycleScope.launch {
            while (true) {
                delay(1000)
                viewModel.tick()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
        _binding = null
    }
}
