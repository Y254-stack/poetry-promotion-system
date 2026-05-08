package com.example.poetry.features.community.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.databinding.FragmentCreatePostBinding
import com.example.poetry.features.community.model.CreatePostData
import com.example.poetry.features.community.repository.CommunityRepositoryImpl
import com.example.poetry.features.community.repository.FollowRepositoryImpl
import com.example.poetry.features.community.viewmodel.CommunityViewModel

class CreatePostFragment : Fragment(R.layout.fragment_create_post) {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommunityViewModel
    private var isPublishing = false  // 防止重复点击

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreatePostBinding.bind(view)

        // 初始化 ViewModel
        val sessionManager = SessionManager(requireContext())
        val repository = CommunityRepositoryImpl(
            com.example.poetry.core.network.NetworkModule.poetryApiService,
            sessionManager
        )
        val followRepository = FollowRepositoryImpl(
            com.example.poetry.core.network.NetworkModule.poetryApiService,
            sessionManager
        )
        viewModel = CommunityViewModel(repository, followRepository)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        // 监听发帖结果
        viewModel.createPostResult.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe

            // 恢复按钮状态
            isPublishing = false
            binding.publishButton.isEnabled = true
            binding.publishButton.text = "发布"

            result.onSuccess {
                Toast.makeText(requireContext(), "发布成功！", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "发布失败，请重试"
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            }
            viewModel.clearCreatePostResult()
        }

        // 监听错误
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
            // 恢复按钮状态
            isPublishing = false
            binding.publishButton.isEnabled = true
            binding.publishButton.text = "发布"
        }
    }

    private fun setupListeners() {
        binding.publishButton.setOnClickListener {
            // 防止重复点击
            if (isPublishing) return@setOnClickListener

            val title = binding.titleInput.text?.toString()?.trim() ?: ""
            val content = binding.contentInput.text?.toString()?.trim() ?: ""
            val tag = binding.tagInput.text?.toString()?.trim() ?: ""

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "请输入标题", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "请输入内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 设置按钮状态
            isPublishing = true
            binding.publishButton.isEnabled = false
            binding.publishButton.text = "发布中..."

            viewModel.createPost(CreatePostData(title, content, tag))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}