package com.example.poetry.features.community.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentPostDetailBinding
import com.example.poetry.features.community.adapter.CommentAdapter
import com.example.poetry.features.community.viewmodel.CommunityViewModel

class PostDetailFragment : Fragment(R.layout.fragment_post_detail) {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommunityViewModel by viewModels()
    private lateinit var adapter: CommentAdapter

    private val postId: Long
        get() = arguments?.getLong("postId", 0L) ?: 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailBinding.bind(view)

        if (postId > 0L) {
            // 后续接入帖子详情接口时可使用 postId 拉取正文与评论
        }

        adapter = CommentAdapter()
        binding.commentRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PostDetailFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(10)))
            isNestedScrollingEnabled = false
        }

        viewModel.comments.observe(viewLifecycleOwner) { adapter.submitList(it) }
        binding.sendCommentButton.setOnClickListener {
            // TODO: 接入真实评论发布逻辑
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
