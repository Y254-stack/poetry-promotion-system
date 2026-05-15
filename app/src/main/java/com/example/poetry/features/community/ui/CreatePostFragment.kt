package com.example.poetry.features.community.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.databinding.FragmentCreatePostBinding
import com.example.poetry.features.community.model.CreatePostData
import com.example.poetry.features.community.repository.CommunityRepositoryImpl
import com.example.poetry.features.community.repository.FollowRepositoryImpl
import com.example.poetry.features.community.viewmodel.CommunityViewModel
import com.example.poetry.features.user.model.DraftUiModel
import com.example.poetry.features.user.viewmodel.UserViewModel
import java.util.Date

class CreatePostFragment : Fragment(R.layout.fragment_create_post) {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private lateinit var communityViewModel: CommunityViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var navController: NavController

    private var draftId: Long = 0L
    private var relatedWorkId: Long? = null
    private var relatedAuthorId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = findNavController()

        val sessionManager = SessionManager(requireContext())
        val repository = CommunityRepositoryImpl(NetworkModule.poetryApiService, sessionManager)
        val followRepository = FollowRepositoryImpl(NetworkModule.poetryApiService, sessionManager)
        communityViewModel = CommunityViewModel(repository, followRepository)
        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]

        setupObservers()
        loadInitialContent()

        binding.cancelButton.setOnClickListener {
            handleCancel()
        }

        binding.publishButton.setOnClickListener {
            publishPost()
        }
    }

    private fun handleCancel() {
        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        val content = binding.contentInput.text?.toString()?.trim().orEmpty()
        val tag = binding.tagInput.text?.toString()?.trim().orEmpty()

        if (title.isEmpty() && content.isEmpty() && tag.isEmpty()) {
            navController.navigateUp()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("保存草稿")
            .setMessage("是否将当前内容保存为草稿？")
            .setPositiveButton("保存") { _, _ ->
                saveDraft()
                navController.navigateUp()
            }
            .setNegativeButton("不保存") { _, _ ->
                navController.navigateUp()
            }
            .setNeutralButton("取消", null)
            .show()
    }

    private fun loadInitialContent() {
        arguments?.let { args ->
            draftId = args.getLong("draft_id", 0L)
            relatedWorkId = args.getLong("source_work_id", 0L).takeIf { it > 0L }
            relatedAuthorId = args.getLong("source_author_id", 0L).takeIf { it > 0L }

            if (draftId > 0L) {
                binding.titleInput.setText(args.getString("draft_title", ""))
                binding.contentInput.setText(args.getString("draft_content", ""))
                binding.tagInput.setText(args.getString("draft_tag", ""))
                return
            }

            val prefillTitle = args.getString("prefill_title").orEmpty()
            val prefillContent = args.getString("prefill_content").orEmpty()
            val prefillTag = args.getString("prefill_tag").orEmpty()

            if (binding.titleInput.text.isNullOrBlank()) {
                binding.titleInput.setText(prefillTitle)
            }
            if (binding.contentInput.text.isNullOrBlank()) {
                binding.contentInput.setText(prefillContent)
            }
            if (binding.tagInput.text.isNullOrBlank()) {
                binding.tagInput.setText(prefillTag)
            }
        }
    }

    private fun saveDraft() {
        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        val content = binding.contentInput.text?.toString()?.trim().orEmpty()
        val tag = binding.tagInput.text?.toString()?.trim().orEmpty()

        if (title.isEmpty() && content.isEmpty() && tag.isEmpty()) {
            return
        }

        val draft = DraftUiModel(
            id = draftId,
            title = title,
            content = content,
            tag = tag.ifEmpty { null },
            updatedAt = Date()
        )
        userViewModel.saveDraft(draft)
        Toast.makeText(requireContext(), "草稿已保存", Toast.LENGTH_SHORT).show()
    }

    private fun publishPost() {
        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        val content = binding.contentInput.text?.toString()?.trim().orEmpty()
        val tag = binding.tagInput.text?.toString()?.trim().orEmpty()

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "请输入标题", Toast.LENGTH_SHORT).show()
            return
        }
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "请输入内容", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionManager = SessionManager(requireContext())
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            navController.navigate(R.id.loginFragment)
            return
        }

        val postData = CreatePostData(
            title = title,
            content = content,
            tag = tag,
            relatedWorkId = relatedWorkId,
            relatedAuthorId = relatedAuthorId
        )
        communityViewModel.createPost(postData)
    }

    private fun setupObservers() {
        communityViewModel.createPostResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                result.onSuccess {
                    Toast.makeText(requireContext(), "发布成功", Toast.LENGTH_SHORT).show()
                    if (draftId > 0L) {
                        userViewModel.deleteDraft(draftId)
                    }
                    navController.navigateUp()
                }.onFailure { exception ->
                    Toast.makeText(
                        requireContext(),
                        "发布失败：${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                communityViewModel.clearCreatePostResult()
            }
        }

        communityViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                communityViewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
