package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentMyFollowsBinding
import com.example.poetry.features.user.adapter.FollowListAdapter
import com.example.poetry.features.user.model.FollowUiModel
import com.example.poetry.features.user.viewmodel.FollowViewModel

class MyFollowsFragment : Fragment(R.layout.fragment_my_follows) {

    private var _binding: FragmentMyFollowsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FollowViewModel by viewModels {
        FollowViewModel.Factory()
    }
    private lateinit var adapter: FollowListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyFollowsBinding.bind(view)

        adapter = FollowListAdapter(
            onOpenProfile = { item ->
                findNavController().navigate(
                    R.id.action_myFollows_to_userPublicProfile,
                    bundleOf("userId" to item.userId)
                )
            },
            onUnfollow = { confirmUnfollow(it) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyFollowsFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
        }

        binding.emptyGoLoginButton.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
        binding.emptyBrowseCommunityButton.setOnClickListener {
            findNavController().navigate(R.id.communitySquareFragment)
        }

        viewModel.followList.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            updateEmptyState()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { updateEmptyState() }
        viewModel.loadError.observe(viewLifecycleOwner) { err ->
            updateEmptyState()
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onResume() {
        super.onResume()
        val auth = SessionManager(requireContext()).bearerAuthorization()
        viewModel.loadFollowing(auth)
    }

    private fun updateEmptyState() {
        val session = SessionManager(requireContext())
        val loggedIn = session.isLoggedIn()
        val rows = viewModel.followList.value.orEmpty()
        val loading = viewModel.isLoading.value == true
        val err = viewModel.loadError.value

        binding.loadingProgress.isVisible = loading

        val showList = rows.isNotEmpty()
        binding.recyclerView.isVisible = showList
        binding.emptyState.isVisible = !showList

        binding.emptyGoLoginButton.isVisible = !loggedIn
        binding.emptyBrowseCommunityButton.isVisible = loggedIn && err == null

        binding.emptyText.text = when {
            loading -> "加载中…"
            !loggedIn -> "请先登录后查看已关注的用户"
            err != null -> "无法加载关注列表"
            else -> "暂无关注，去内容广场发现感兴趣的用户吧"
        }
    }

    private fun confirmUnfollow(item: FollowUiModel) {
        AlertDialog.Builder(requireContext())
            .setMessage("确认取消关注「${item.displayName}」？")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val auth = SessionManager(requireContext()).bearerAuthorization()
                viewModel.unfollow(auth, item.userId) { ok ->
                    if (ok) {
                        Toast.makeText(requireContext(), "已取消关注", Toast.LENGTH_SHORT).show()
                        viewModel.loadFollowing(auth)
                    } else {
                        Toast.makeText(requireContext(), "操作失败，请稍后重试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
