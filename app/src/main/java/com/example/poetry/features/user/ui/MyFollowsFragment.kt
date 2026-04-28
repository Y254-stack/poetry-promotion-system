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
    }

    private fun confirmUnfollow(item: FollowUiModel) {
        AlertDialog.Builder(requireContext())
            .setMessage("确认取消关注「${item.displayName}」？")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.unfollow(item.userId)
                Toast.makeText(requireContext(), "已取消关注", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
