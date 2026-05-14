package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poetry.R
import com.example.poetry.databinding.FragmentQuizResultBinding

class QuizResultFragment : Fragment(R.layout.fragment_quiz_result) {

    private var _binding: FragmentQuizResultBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuizResultBinding.bind(view)

        // 获取传递过来的数据
        val correct = arguments?.getInt("correctCount", 0) ?: 0
        val wrong = arguments?.getInt("wrongCount", 0) ?: 0
        val total = arguments?.getInt("totalQuestions", 0) ?: 0
        val totalSeconds = arguments?.getInt("elapsedSeconds", 0) ?: 0

        // 计算分数（正确一题5分）
        val score = correct * 5
        val accuracy = if (total > 0) (correct * 100 / total) else 0

        // 格式化时间
        val minutes = totalSeconds / 60
        val remainingSeconds = totalSeconds % 60
        val timeStr = String.format("%02d:%02d", minutes, remainingSeconds)

        // 显示数据
        binding.tvScore.text = score.toString()
        binding.tvTotalQuestions.text = total.toString()
        binding.tvCorrectCount.text = correct.toString()
        binding.tvWrongCount.text = wrong.toString()
        binding.tvAccuracy.text = "$accuracy%"
        binding.tvTimeSpent.text = timeStr

        // 重新答题按钮
        binding.btnRestart.setOnClickListener {
            // 返回到 QuizFragment 并重新开始
            findNavController().navigate(R.id.quizFragment)
        }

        // 返回首页按钮
        binding.btnExit.setOnClickListener {
            // 返回到趣味学习页面
            findNavController().navigate(R.id.learningHubFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}