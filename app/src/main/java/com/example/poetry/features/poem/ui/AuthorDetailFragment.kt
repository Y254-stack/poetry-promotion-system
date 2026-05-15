package com.example.poetry.features.poem.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentAuthorDetailBinding
import com.example.poetry.features.poem.adapter.PoemSummaryAdapter
import com.example.poetry.features.poem.viewmodel.PoemViewModel

class AuthorDetailFragment : Fragment(R.layout.fragment_author_detail) {

    private var _binding: FragmentAuthorDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PoemViewModel by viewModels()
    private lateinit var worksAdapter: PoemSummaryAdapter

    private var authorId: Long = 0L
    private var fallbackAuthorName: String = ""
    private var fallbackDynastyName: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAuthorDetailBinding.bind(view)

        authorId = arguments?.getLong("authorId", 0L) ?: 0L
        fallbackAuthorName = arguments?.getString("authorName").orEmpty()
        fallbackDynastyName = arguments?.getString("dynastyName").orEmpty()

        binding.nameText.text = fallbackAuthorName
        binding.dynastyText.text = fallbackDynastyName
        binding.introText.text = "\u6b63\u5728\u52a0\u8f7d\u4f5c\u8005\u7b80\u4ecb..."

        worksAdapter = PoemSummaryAdapter {
            findNavController().navigate(
                R.id.poemDetailFragment,
                bundleOf("workId" to it.workId)
            )
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = worksAdapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(10)))
            isNestedScrollingEnabled = false
        }

        binding.viewAllWorksButton.setOnClickListener {
            if (authorId <= 0L) return@setOnClickListener
            val currentName = viewModel.authorProfile.value?.name?.takeIf { it.isNotBlank() } ?: fallbackAuthorName
            findNavController().navigate(
                R.id.searchResultFragment,
                bundleOf(
                    "searchType" to "AUTHOR_ID",
                    "authorId" to authorId,
                    "authorName" to currentName
                )
            )
        }

        viewModel.authorProfile.observe(viewLifecycleOwner) { profile ->
            binding.nameText.text = profile.name.ifBlank { fallbackAuthorName }
            binding.dynastyText.text = profile.dynasty.ifBlank { fallbackDynastyName }
            binding.introText.text = profile.intro.ifBlank { "\u6682\u65e0\u4f5c\u8005\u7b80\u4ecb" }
            binding.worksTitleText.text = if (profile.workCount > 0) {
                "\u4f5c\u54c1\u5217\u8868\uff08\u5171 ${profile.workCount} \u9996\uff09"
            } else {
                "\u4f5c\u54c1\u5217\u8868"
            }
            binding.viewAllWorksButton.isVisible = authorId > 0L && profile.workCount > 0
        }

        viewModel.representativeWorks.observe(viewLifecycleOwner) { works ->
            worksAdapter.submitList(works)
            updateWorksEmptyState()
        }

        viewModel.authorDetailLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            updateWorksEmptyState()
        }

        viewModel.authorDetailErrorMessage.observe(viewLifecycleOwner) { error ->
            binding.errorText.isVisible = !error.isNullOrBlank()
            binding.errorText.text = error.orEmpty()
            updateWorksEmptyState()
        }

        if (authorId > 0L) {
            viewModel.loadAuthorDetail(authorId)
        } else {
            binding.errorText.isVisible = true
            binding.errorText.text = "\u7f3a\u5c11\u4f5c\u8005\u7f16\u53f7\uff0c\u65e0\u6cd5\u52a0\u8f7d\u4f5c\u8005\u8be6\u60c5"
            updateWorksEmptyState()
        }
    }

    private fun updateWorksEmptyState() {
        val loading = viewModel.authorDetailLoading.value ?: false
        val hasError = !viewModel.authorDetailErrorMessage.value.isNullOrBlank()
        val hasWorks = !viewModel.representativeWorks.value.isNullOrEmpty()
        binding.emptyWorksText.isVisible = !loading && !hasError && !hasWorks
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
