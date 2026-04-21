package com.example.poetry.features.auth.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.poetry.R
import com.example.poetry.databinding.FragmentForgotPasswordBinding

class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentForgotPasswordBinding.bind(view)

        binding.sendCodeButton.setOnClickListener {
            // TODO: 接入真实验证码发送
        }
        binding.resetButton.setOnClickListener {
            // TODO: 接入真实密码重置
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
