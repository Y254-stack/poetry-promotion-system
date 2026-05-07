package com.example.poetry.features.community.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.databinding.FragmentPostDetailBinding
import com.example.poetry.features.community.repository.CommunityRepositoryImpl
import com.example.poetry.features.community.viewmodel.CommunityViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailFragment : Fragment(R.layout.fragment_post_detail) {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommunityViewModel
    private var postId: Long = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailBinding.bind(view)

        // 获取帖子ID
        postId = arguments?.getLong("post_id", -1) ?: -1
        if (postId == -1L) {
            Toast.makeText(requireContext(), "帖子不存在", Toast.LENGTH_SHORT).show()
            return
        }

        // 初始化 ViewModel
        val sessionManager = SessionManager(requireContext())
        val repository = CommunityRepositoryImpl(
            com.example.poetry.core.network.NetworkModule.poetryApiService,
            sessionManager
        )
        viewModel = CommunityViewModel(repository)

        setupObservers()

        // 加载帖子详情
        viewModel.loadPostDetail(postId)
    }

    private fun setupObservers() {
        // 帖子详情
        viewModel.postDetail.observe(viewLifecycleOwner) { post ->
            post?.let {
                binding.titleText.text = it.title
                binding.authorInfoText.text = "${it.author} · ${formatTime(it.createdAt)}"
                binding.contentText.text = it.contentText
                binding.tagText.text = it.tag.ifEmpty { "分享" }
                binding.tagText.visibility = if (it.tag.isNotEmpty()) View.VISIBLE else View.GONE
                binding.statsText.text = "${it.viewCount}阅读 · ${it.commentCount}评论 · ${it.likeCount}点赞"
            }
        }

        // 加载状态
        viewModel.isLoadingDetail.observe(viewLifecycleOwner) { isLoading ->
            // 可以显示 loading，暂时不做处理
        }

        // 错误
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun formatTime(date: Date): String {
        val now = Date()
        val diff = now.time - date.time

        return when {
            diff < 60000 -> "刚刚"
            diff < 3600000 -> "${diff / 60000}分钟前"
            diff < 86400000 -> "${diff / 3600000}小时前"
            else -> {
                val format = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                format.format(date)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}