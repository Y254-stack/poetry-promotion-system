package com.example.poetry.features.auth.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentLoginBinding
import com.example.poetry.features.auth.adapter.AuthTipsAdapter
import com.example.poetry.features.auth.viewmodel.AuthViewModel

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var tipsAdapter: AuthTipsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        tipsAdapter = AuthTipsAdapter()
        binding.tipsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tipsAdapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(10)))
            isNestedScrollingEnabled = false
        }

        viewModel.tips.observe(viewLifecycleOwner) { tipsAdapter.submitList(it) }

        binding.loginButton.setOnClickListener {
            // TODO: 接入真实登录校验
        }
        binding.registerText.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
        binding.forgotPasswordText.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgotPassword)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
