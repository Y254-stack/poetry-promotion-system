package com.example.poetry.features.community.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentCommunitySquareBinding
import com.example.poetry.features.community.adapter.PostAdapter
import com.example.poetry.features.community.repository.CommunityRepositoryImpl
import com.example.poetry.features.community.viewmodel.CommunityViewModel
import kotlinx.coroutines.launch

class CommunitySquareFragment : Fragment(R.layout.fragment_community_square) {

    private var _binding: FragmentCommunitySquareBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommunityViewModel
    private lateinit var adapter: PostAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCommunitySquareBinding.bind(view)

        // 初始化 ViewModel
        val sessionManager = SessionManager(requireContext())
        val repository = CommunityRepositoryImpl(
            com.example.poetry.core.network.NetworkModule.poetryApiService,
            sessionManager
        )
        viewModel = CommunityViewModel(repository)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // 加载数据
        viewModel.loadPosts()
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter { post ->
            val bundle = Bundle().apply {
                putLong("post_id", post.postId)
            }
            findNavController().navigate(R.id.action_community_to_postDetail, bundle)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CommunitySquareFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
        }
    }

    private fun setupObservers() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
        }

        viewModel.isLoadingPosts.observe(viewLifecycleOwner) { isLoading ->
            // 可以显示 loading，暂时不做处理
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // 发帖结果
        viewModel.createPostResult.observe(viewLifecycleOwner) { result ->
            result?.onSuccess {
                Toast.makeText(requireContext(), "发布成功", Toast.LENGTH_SHORT).show()
                binding.recyclerView.smoothScrollToPosition(0)
            }?.onFailure {
                Toast.makeText(requireContext(), it.message ?: "发布失败", Toast.LENGTH_SHORT).show()
            }
            viewModel.clearCreatePostResult()
        }
    }

    private fun setupListeners() {
        binding.createPostButton.setOnClickListener {
            findNavController().navigate(R.id.action_community_to_createPost)
        }
        binding.notificationButton.setOnClickListener {
            findNavController().navigate(R.id.action_community_to_notifications)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}