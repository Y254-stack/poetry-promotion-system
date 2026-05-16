package com.example.poetry.features.auth.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.poetry.R
import com.example.poetry.databinding.FragmentForgotPasswordBinding
import com.example.poetry.features.auth.viewmodel.AuthViewModel

class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentForgotPasswordBinding.bind(view)

        // Observe send verification code result
        authViewModel.codeSent.observe(viewLifecycleOwner) { sent ->
            if (sent) {
                Toast.makeText(requireContext(), "验证码已发送", Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.sendCodeError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        // Observe reset password result
        authViewModel.resetSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "密码重置成功", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        authViewModel.resetError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        // Loading state
        authViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.sendCodeButton.isEnabled = !loading
            binding.resetButton.isEnabled = !loading
        }

        // Send verification code button
        binding.sendCodeButton.setOnClickListener {
            val email = binding.emailInput.text?.toString()?.trim() ?: ""
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "请输入邮箱", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authViewModel.sendVerificationCode(email)
        }

        // Reset password button
        binding.resetButton.setOnClickListener {
            val email = binding.emailInput.text?.toString()?.trim() ?: ""
            val code = binding.verificationCodeInput.text?.toString()?.trim() ?: ""
            val newPassword = binding.newPasswordInput.text?.toString() ?: ""
            val confirmPassword = binding.confirmPasswordInput.text?.toString() ?: ""

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "请输入邮箱", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "请输入验证码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword.isEmpty()) {
                Toast.makeText(requireContext(), "请输入新密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.resetPassword(email, code, newPassword, confirmPassword)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
