package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.poetry.R
import com.example.poetry.databinding.FragmentFillBlankBinding

class FillBlankFragment : Fragment(R.layout.fragment_fill_blank) {

    private var _binding: FragmentFillBlankBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFillBlankBinding.bind(view)
        binding.submitButton.setOnClickListener {
            // TODO: 接入真实判题逻辑
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
