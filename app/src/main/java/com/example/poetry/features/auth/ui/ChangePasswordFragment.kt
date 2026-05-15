package com.example.poetry.features.auth.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.poetry.R
import com.example.poetry.databinding.FragmentChangePasswordBinding
import com.example.poetry.features.auth.viewmodel.AuthViewModel

class ChangePasswordFragment : Fragment(R.layout.fragment_change_password) {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChangePasswordBinding.bind(view)

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.changePasswordButton.isEnabled = !loading
            binding.changePasswordButton.text = if (loading) "修改中..." else "修改密码"
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                val session = com.example.poetry.core.auth.SessionManager(requireContext())
                session.saveSession(it.token, it.userId, it.username, it.nickname, it.avatarUrl)
                Toast.makeText(requireContext(), "密码修改成功", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

        binding.changePasswordButton.setOnClickListener {
            val currentPassword = binding.currentPasswordEdit.text?.toString().orEmpty()
            val newPassword = binding.newPasswordEdit.text?.toString().orEmpty()
            val confirmPassword = binding.confirmPasswordEdit.text?.toString().orEmpty()
            
            // 从SessionManager获取token
            val session = com.example.poetry.core.auth.SessionManager(requireContext())
            val token = session.token() ?: ""
            viewModel.changePassword(token, currentPassword, newPassword, confirmPassword)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}