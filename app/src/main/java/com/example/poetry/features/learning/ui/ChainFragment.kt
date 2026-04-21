package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.poetry.R
import com.example.poetry.databinding.FragmentChainBinding

class ChainFragment : Fragment(R.layout.fragment_chain) {

    private var _binding: FragmentChainBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChainBinding.bind(view)
        binding.submitButton.setOnClickListener {
            // TODO: 接入真实接龙校验
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
