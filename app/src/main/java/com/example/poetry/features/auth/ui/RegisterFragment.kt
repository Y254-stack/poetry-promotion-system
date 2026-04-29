package com.example.poetry.features.auth.ui

import android.widget.Toast
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.poetry.R
import com.example.poetry.databinding.FragmentRegisterBinding
import com.example.poetry.features.auth.viewmodel.AuthViewModel

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.registerButton.isEnabled = !loading
            binding.registerButton.text = if (loading) "注册中..." else getString(R.string.action_register)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.authResult.observe(viewLifecycleOwner) { auth ->
            auth?.let {
                val session = com.example.poetry.core.auth.SessionManager(requireContext())
                session.saveSession(it.token, it.userId, it.username, it.nickname)
                Toast.makeText(requireContext(), getString(R.string.toast_register_success), Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

        binding.registerButton.setOnClickListener {
            val username = binding.accountEdit.text?.toString().orEmpty()
            val nickname = binding.nicknameEdit.text?.toString().orEmpty()
            val email = binding.emailEdit.text?.toString().orEmpty()
            val password = binding.passwordEdit.text?.toString().orEmpty()
            val confirm = binding.confirmEdit.text?.toString().orEmpty()
            viewModel.register(
                username = username,
                nickname = nickname,
                email = email,
                password = password,
                confirm = confirm
            )
        }

        binding.goLoginText.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
