package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.poetry.R
import com.example.poetry.databinding.FragmentQuizBinding

class QuizFragment : Fragment(R.layout.fragment_quiz) {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuizBinding.bind(view)
        binding.submitButton.setOnClickListener {
            // TODO: 接入真实小测提交流程
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
