package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
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
import com.example.poetry.features.user.viewmodel.UserViewModel

class MyFollowsFragment : Fragment(R.layout.fragment_my_follows) {

    private var _binding: FragmentMyFollowsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: FollowListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyFollowsBinding.bind(view)

        val session = SessionManager(requireContext())
        viewModel.setSessionToken(session.token())

        adapter = FollowListAdapter(
            onOpenProfile = { item ->
                findNavController().navigate(
                    R.id.action_myFollows_to_userPublicProfile,
                    bundleOf(
                        "userId" to item.userId,
                        "displayName" to item.displayName
                    )
                )
            },
            onUnfollow = { confirmUnfollow(it) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyFollowsFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
        }

        viewModel.follows.observe(viewLifecycleOwner) { adapter.submitList(it) }
        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun confirmUnfollow(item: FollowUiModel) {
        AlertDialog.Builder(requireContext())
            .setMessage("确认取消关注「${item.displayName}」？")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val followedUserId = item.userId.toLongOrNull()
                if (followedUserId == null) {
                    Toast.makeText(requireContext(), "用户ID无效", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.unfollow(followedUserId) { ok ->
                    if (ok) Toast.makeText(requireContext(), "已取消关注", Toast.LENGTH_SHORT).show()
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
