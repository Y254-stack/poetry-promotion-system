package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.databinding.FragmentUserPublicProfileBinding
import com.example.poetry.features.community.adapter.PostAdapter
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.community.repository.CommunityRepositoryImpl
import com.example.poetry.features.community.repository.FollowRepositoryImpl
import com.example.poetry.features.community.viewmodel.CommunityViewModel

class UserPublicProfileFragment : Fragment(R.layout.fragment_user_public_profile) {

    private var _binding: FragmentUserPublicProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommunityViewModel
    private lateinit var postAdapter: PostAdapter
    private var targetUserId: Long = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUserPublicProfileBinding.bind(view)

        // 获取目标用户ID
        targetUserId = arguments?.getLong("userId", 0) ?: 0
        val displayName = arguments?.getString("displayName").orEmpty()

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

        // 初始化帖子列表适配器
        postAdapter = PostAdapter(
            onClick = { post ->
                // 帖子点击跳转到详情页
                val bundle = Bundle().apply {
                    putLong("post_id", post.postId)
                }
                findNavController().navigate(R.id.action_userPublicProfile_to_postDetail, bundle)
            },
            onAuthorClick = { _, _ -> /* 作者点击事件 */ }
        )
        binding.postsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.postsRecyclerView.adapter = postAdapter

        // 设置初始昵称
        binding.nicknameText.text = displayName.ifEmpty { "用户" }

        // 关注按钮点击事件
        binding.followButton.setOnClickListener {
            // 检查是否关注自己
            val currentUserId = sessionManager.userId()
            if (currentUserId == targetUserId) {
                Toast.makeText(requireContext(), "不能关注自己", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.followUser(targetUserId)
        }

        // 观察用户资料
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.nicknameText.text = it.nickname
                binding.usernameText.text = "@${it.username}"
                binding.bioText.text = it.bio ?: "暂无简介"
                binding.likeCountText.text = it.likeCount.toString()
                binding.followingCountText.text = it.followingCount.toString()
                binding.followerCountText.text = it.followerCount.toString()
                
                // 设置头像首字母
                binding.avatarText.text = it.nickname.take(1)
                
                updateFollowButton(it.isFollowing)
            }
        }

        // 观察用户帖子
        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            if (posts.isEmpty()) {
                binding.postsRecyclerView.visibility = View.GONE
                binding.emptyText.visibility = View.VISIBLE
            } else {
                binding.postsRecyclerView.visibility = View.VISIBLE
                binding.emptyText.visibility = View.GONE
                postAdapter.submitList(posts)
            }
        }

        // 观察关注状态变化
        viewModel.followActionResult.observe(viewLifecycleOwner) { result ->
            result?.onSuccess { response ->
                updateFollowButton(response.isFollowing)
                // 更新粉丝数（当前页面显示的是被关注用户的信息，所以应该更新粉丝数）
                binding.followerCountText.text = response.followerCount.toString()
                Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
            }?.onFailure { exception ->
                val errorMessage = exception.message
                if (errorMessage?.contains("不能关注自己") == true) {
                    Toast.makeText(requireContext(), "不能关注自己", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "关注操作失败", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 观察错误
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                if (it.contains("不能关注自己")) {
                    Toast.makeText(requireContext(), "不能关注自己", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
                viewModel.clearError()
            }
        }

        // 加载用户资料和帖子
        viewModel.loadUserProfile(targetUserId)
        viewModel.loadUserPosts(targetUserId)
    }

    private fun updateFollowButton(isFollowing: Boolean) {
        if (isFollowing) {
            binding.followButton.text = "已关注"
            binding.followButton.setBackgroundResource(R.drawable.bg_follow_button_filled)
            binding.followButton.setTextColor(resources.getColor(R.color.white))
        } else {
            binding.followButton.text = "关注"
            binding.followButton.setBackgroundResource(R.drawable.bg_follow_button)
            binding.followButton.setTextColor(resources.getColor(R.color.teal_700))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}