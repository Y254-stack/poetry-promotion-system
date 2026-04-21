package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.poetry.R
import com.example.poetry.databinding.FragmentFeihuaBinding

class FeihuaFragment : Fragment(R.layout.fragment_feihua) {

    private var _binding: FragmentFeihuaBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFeihuaBinding.bind(view)
        binding.submitButton.setOnClickListener {
            // TODO: 接入真实飞花令判题与计时
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
