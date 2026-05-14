package com.example.poetry.features.community.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.databinding.FragmentPostDetailBinding
import com.example.poetry.features.community.adapter.CommentAdapter
import com.example.poetry.features.community.model.CommentUiModel
import com.example.poetry.features.community.model.PostDetailUiModel
import com.example.poetry.features.community.repository.CommunityRepositoryImpl
import com.example.poetry.features.community.repository.FollowRepositoryImpl
import com.example.poetry.features.community.viewmodel.CommunityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailFragment : Fragment(R.layout.fragment_post_detail) {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommunityViewModel
    private var postId: Long = -1
    private lateinit var commentAdapter: CommentAdapter
    private var currentUserId: Long? = null
    private var replyToComment: CommentUiModel? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailBinding.bind(view)

        postId = arguments?.getLong("post_id", -1) ?: -1
        if (postId == -1L) {
            Toast.makeText(requireContext(), "帖子不存在", Toast.LENGTH_SHORT).show()
            return
        }

        initViewModel()
        setupRecyclerView()
        setupActionButtons()
        setupInputBox()
        setupObservers()

        viewModel.loadPostDetail(postId)
        viewModel.loadComments(postId)
    }

    private fun initViewModel() {
        val sessionManager = SessionManager(requireContext())
        // 获取当前用户ID
        currentUserId = sessionManager.userId()
        val repository = CommunityRepositoryImpl(
            com.example.poetry.core.network.NetworkModule.poetryApiService,
            sessionManager
        )
        val followRepository = FollowRepositoryImpl(
            com.example.poetry.core.network.NetworkModule.poetryApiService,
            sessionManager
        )
        viewModel = CommunityViewModel(repository, followRepository)
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(
            currentUserId = currentUserId,
            onReplyClick = { comment ->
                replyToComment = comment
                binding.commentEditText.hint = "回复 @${comment.author}:"
                binding.commentEditText.requestFocus()
            },
            onLikeClick = { comment ->
                viewModel.likeComment(postId, comment.commentId)
            },
            onDeleteClick = { comment ->
                // 显示确认对话框
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("确认删除")
                    .setMessage("确定要删除这条评论吗？")
                    .setPositiveButton("删除") { _, _ ->
                        viewModel.deleteComment(postId, comment.commentId)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            },
            onAuthorClick = { userId, authorName ->
                val bundle = Bundle().apply {
                    putLong("userId", userId)
                    putString("displayName", authorName)
                }
                findNavController().navigate(R.id.action_postDetail_to_userPublicProfile, bundle)
            }
        )
        
        binding.commentRecycler.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = commentAdapter
        }
    }

    private fun setupActionButtons() {
        binding.btnLike.setOnClickListener {
            // 调用 ViewModel 的点赞方法
            viewModel.likePost(postId)
        }

        binding.btnCollect.setOnClickListener {
            // 调用 ViewModel 的收藏方法
            viewModel.collectPost(postId)
        }
    }

    private fun setupInputBox() {
        binding.commentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSendButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.sendCommentButton.setOnClickListener {
                val content = binding.commentEditText.text.toString()
                if (content.isNotBlank()) {
                    // 如果是回复评论，传递 parentCommentId 和 replyUserId
                    val parentComment = replyToComment
                    if (parentComment != null) {
                        viewModel.createComment(
                            postId = postId,
                            content = content,
                            parentCommentId = parentComment.commentId,
                            replyUserId = parentComment.userId
                        )
                    } else {
                        viewModel.createComment(postId, content)
                    }
                }
            }
    }

    private fun updateSendButtonState() {
        val isCreating = viewModel.isCreatingComment.value ?: false
        val content = binding.commentEditText.text
        binding.sendCommentButton.isEnabled = !isCreating && !content.isNullOrBlank()
    }

    private fun setupObservers() {
        // 帖子详情加载状态
        viewModel.isLoadingDetail.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 帖子内容
        viewModel.postDetail.observe(viewLifecycleOwner) { post ->
            post?.let { updatePostUI(it) }
        }

        // 评论列表
        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            // 调试：打印评论数量
            println("DEBUG: PostDetailFragment - 收到 ${comments.size} 条顶级评论")
            // 使用新方法提交评论（包含子评论扁平化和层级缩进）
            commentAdapter.submitCommentsWithReplies(comments)
            // 强制刷新 RecyclerView
            commentAdapter.notifyDataSetChanged()
        }

        // 发布评论状态
        viewModel.isCreatingComment.observe(viewLifecycleOwner) { isCreating ->
            binding.sendCommentButton.text = if (isCreating) "发送中..." else "发送"
            updateSendButtonState()
        }

        // 评论发送结果
        viewModel.createCommentResult.observe(viewLifecycleOwner) { result ->
            result?.onSuccess {
                binding.commentEditText.setText("")
                binding.commentEditText.hint = "写下你的精彩评论..."
                replyToComment = null  // 重置回复状态
                Toast.makeText(requireContext(), "评论成功", Toast.LENGTH_SHORT).show()
                viewModel.clearCreateCommentResult()
            }?.onFailure {
                Toast.makeText(requireContext(), "评论失败: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 错误处理
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun updatePostUI(post: PostDetailUiModel) {
        binding.postNickname.text = post.author
        binding.postTime.text = formatTime(post.createdAt)
        binding.postTitle.text = post.title
        binding.postContent.text = post.contentText
        binding.tvLikeCount.text = post.likeCount.toString()
        binding.tvCollectCount.text = post.collectCount.toString()
        binding.postAvatar.text = if (post.author.isNotEmpty()) post.author.take(1).uppercase() else "?"
        
        // 更新点赞和收藏状态
        binding.ivLike.isSelected = post.isLiked
        binding.ivCollect.isSelected = post.isCollected
        
        // 头像点击跳转到用户主页
        binding.postAvatar.setOnClickListener {
            navigateToUserProfile(post.userId, post.author)
        }
        
        // 昵称点击跳转到用户主页
        binding.postNickname.setOnClickListener {
            navigateToUserProfile(post.userId, post.author)
        }
    }
    
    private fun navigateToUserProfile(userId: Long, displayName: String) {
        val bundle = Bundle().apply {
            putLong("userId", userId)
            putString("displayName", displayName)
        }
        findNavController().navigate(R.id.action_postDetail_to_userPublicProfile, bundle)
    }

    private fun formatTime(date: Date): String {
        val now = Date()
        val diff = now.time - date.time
        return when {
            diff < 60000 -> "刚刚"
            diff < 3600000 -> "${diff / 60000}分钟前"
            diff < 86400000 -> "${diff / 3600000}小时前"
            else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}