package com.example.poetry.features.poem.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentPoemDetailBinding
import com.example.poetry.features.poem.adapter.PoemSummaryAdapter
import com.example.poetry.features.poem.viewmodel.PoemViewModel

class PoemDetailFragment : Fragment(R.layout.fragment_poem_detail) {

    private var _binding: FragmentPoemDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PoemViewModel by viewModels()
    private lateinit var relatedAdapter: PoemSummaryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPoemDetailBinding.bind(view)

        relatedAdapter = PoemSummaryAdapter {
            findNavController().navigate(
                R.id.poemDetailFragment,
                bundleOf("workId" to it.workId)
            )
        }
        binding.relatedRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = relatedAdapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(10)))
            isNestedScrollingEnabled = false
        }

        val workId = arguments?.getLong("workId", 0L) ?: 0L
        if (workId > 0) {
            viewModel.loadPoemDetail(workId)
            viewModel.checkFavoriteStatus(1L, workId)
        }

        viewModel.poemDetail.observe(viewLifecycleOwner) { detail ->
            binding.titleText.text = detail.title
            binding.authorButton.text = "${detail.author} · ${detail.dynasty}"
            binding.contentText.text = detail.content
            binding.translationText.text = detail.translation.ifBlank { "暂无译文" }
            binding.annotationText.text = detail.annotation.ifBlank { "暂无注释" }
            binding.appreciationText.text = detail.appreciation.ifBlank { "暂无赏析" }
        }

        viewModel.isFavorited.observe(viewLifecycleOwner) { isFavorited ->
            updateFavoriteButton(isFavorited)
        }

        binding.favoriteButton.setOnClickListener {
            if (workId > 0) {
                val currentStatus = viewModel.isFavorited.value ?: false
                viewModel.toggleFavorite(1L, workId)
                Toast.makeText(
                    requireContext(),
                    if (currentStatus) "已取消收藏" else "已收藏",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.authorButton.setOnClickListener {
            findNavController().navigate(R.id.action_poemDetail_to_authorDetail)
        }
    }

    private fun updateFavoriteButton(isFavorited: Boolean) {
        binding.favoriteButton.setImageResource(
            if (isFavorited) android.R.drawable.star_big_on
            else android.R.drawable.star_big_off
        )
        binding.favoriteButton.setColorFilter(
            if (isFavorited) Color.parseColor("#FF9800")
            else Color.parseColor("#999999")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
