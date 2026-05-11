package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentMyCreationsBinding
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.user.adapter.CreationListAdapter
import com.example.poetry.features.user.model.DraftUiModel
import com.example.poetry.features.user.viewmodel.UserViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyCreationsFragment : Fragment(R.layout.fragment_my_creations) {

    private var _binding: FragmentMyCreationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: CreationListAdapter
    private var showingDrafts = false
    private var isDeleting = false  // 防止重复删除

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyCreationsBinding.bind(view)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        refreshData()
    }

    private fun setupRecyclerView() {
        adapter = CreationListAdapter(
            onOpen = { item ->
                if (isDeleting) return@CreationListAdapter  // 删除中禁止操作
                when (item) {
                    is CommunityPostUiModel -> {
                        val bundle = Bundle().apply {
                            putLong("post_id", item.postId)
                        }
                        findNavController().navigate(R.id.postDetailFragment, bundle)
                    }
                    is DraftUiModel -> {
                        val bundle = Bundle().apply {
                            putLong("draft_id", item.id)
                            putString("draft_title", item.title)
                            putString("draft_content", item.content)
                            putString("draft_tag", item.tag ?: "")
                        }
                        findNavController().navigate(R.id.createPostFragment, bundle)
                    }
                }
            },
            onDelete = { item ->
                if (isDeleting) return@CreationListAdapter  // 防止重复删除
                when (item) {
                    is CommunityPostUiModel -> showDeleteConfirmDialog(item)
                    is DraftUiModel -> showDeleteDraftConfirmDialog(item)
                }
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyCreationsFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
        }
    }

    private fun setupObservers() {
        // 已发布帖子
        viewModel.publishedPosts.observe(viewLifecycleOwner) { posts ->
            if (!showingDrafts && !isDeleting) {
                adapter.submitList(posts ?: emptyList())
            }
        }

        // 草稿
        viewModel.drafts.observe(viewLifecycleOwner) { drafts ->
            if (showingDrafts && !isDeleting) {
                adapter.submitList(drafts ?: emptyList())
            }
        }

        // 删除帖子结果
        viewModel.deletePostResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                isDeleting = false  // 重置删除标志
                if (it.isSuccess) {
                    Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
                    // 延迟一点刷新，确保 UI 流畅
                    lifecycleScope.launch {
                        delay(100)
                        refreshData()
                    }
                } else {
                    Toast.makeText(requireContext(), it.exceptionOrNull()?.message ?: "删除失败", Toast.LENGTH_SHORT).show()
                }
                viewModel.clearDeletePostResult()
            }
        }

        // 删除草稿结果
        viewModel.deleteDraftResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                isDeleting = false
                if (it.isSuccess) {
                    Toast.makeText(requireContext(), "草稿已删除", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), it.exceptionOrNull()?.message ?: "删除失败", Toast.LENGTH_SHORT).show()
                }
                viewModel.clearDeleteDraftResult()
            }
        }

        // 错误
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                isDeleting = false
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupListeners() {
        binding.tabLayout.removeAllTabs()
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("已发布"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("草稿"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showingDrafts = tab?.position == 1
                if (!showingDrafts) {
                    refreshData()
                } else {
                    adapter.submitList(viewModel.drafts.value ?: emptyList())
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.fabAdd.setOnClickListener {
            if (isDeleting) return@setOnClickListener
            try {
                findNavController().navigate(R.id.action_myCreations_to_createPost)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "无法跳转", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmDialog(post: CommunityPostUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除帖子")
            .setMessage("确认删除「${post.title}」吗？删除后无法恢复。")
            .setPositiveButton("删除") { dialog, _ ->
                isDeleting = true
                // 先关闭对话框
                dialog.dismiss()
                // 立即从列表中移除（乐观更新）
                val currentList = viewModel.publishedPosts.value?.toMutableList() ?: mutableListOf()
                currentList.removeAll { it.postId == post.postId }
                adapter.submitList(currentList)
                // 调用删除 API
                viewModel.deletePost(post.postId)
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                // 确保对话框关闭后不会卡住
            }
            .show()
    }

    private fun showDeleteDraftConfirmDialog(draft: DraftUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除草稿")
            .setMessage("确认删除草稿「${draft.title.ifEmpty { "无标题" }}」吗？")
            .setPositiveButton("删除") { dialog, _ ->
                isDeleting = true
                dialog.dismiss()
                // 立即从列表中移除
                val currentList = viewModel.drafts.value?.toMutableList() ?: mutableListOf()
                currentList.removeAll { it.id == draft.id }
                adapter.submitList(currentList)
                // 调用删除 API
                viewModel.deleteDraft(draft.id)
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun refreshData() {
        val sessionManager = SessionManager(requireContext())
        if (sessionManager.isLoggedIn()) {
            viewModel.loadPublishedPosts(sessionManager.userId(), refresh = true)
        } else {
            adapter.submitList(emptyList())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}