package com.example.poetry.features.auth.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.poetry.R
import com.example.poetry.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        binding.registerButton.setOnClickListener {
            // TODO: 接入真实注册逻辑
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
