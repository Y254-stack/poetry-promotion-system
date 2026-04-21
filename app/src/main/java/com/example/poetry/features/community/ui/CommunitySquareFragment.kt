package com.example.poetry.features.community.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentCommunitySquareBinding
import com.example.poetry.features.community.adapter.PostAdapter
import com.example.poetry.features.community.viewmodel.CommunityViewModel

class CommunitySquareFragment : Fragment(R.layout.fragment_community_square) {

    private var _binding: FragmentCommunitySquareBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommunityViewModel by viewModels()
    private lateinit var adapter: PostAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCommunitySquareBinding.bind(view)

        adapter = PostAdapter {
            // TODO: 传递真实帖子ID
            findNavController().navigate(R.id.action_community_to_postDetail)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CommunitySquareFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
        }

        viewModel.posts.observe(viewLifecycleOwner) { adapter.submitList(it) }

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
