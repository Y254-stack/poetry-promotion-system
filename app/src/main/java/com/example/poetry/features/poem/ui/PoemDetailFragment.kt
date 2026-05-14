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
import com.example.poetry.core.auth.SessionManager
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

        setupStickyContentCard()

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
            val auth = SessionManager(requireContext()).bearerAuthorization()
            viewModel.checkFavoriteStatus(auth, workId)
        }

        viewModel.poemDetail.observe(viewLifecycleOwner) { detail ->
            binding.titleText.text = detail.title
            binding.authorButton.text = "${detail.author} · ${detail.dynasty}"

            // 智能格式化原文
            val formattedContent = formatPoemContent(detail.content)
            binding.contentText.text = formattedContent
            binding.stickyContentText.text = formattedContent

            binding.translationText.text = detail.translation.ifBlank { "暂无译文" }
            binding.annotationText.text = detail.annotation.ifBlank { "暂无注释" }
            binding.appreciationText.text = detail.appreciation.ifBlank { "暂无赏析" }
        }

        viewModel.isFavorited.observe(viewLifecycleOwner) { isFavorited ->
            updateFavoriteButton(isFavorited)
        }

        binding.favoriteButton.setOnClickListener {
            if (workId <= 0) return@setOnClickListener
            val auth = SessionManager(requireContext()).bearerAuthorization()
            if (auth.isNullOrBlank()) {
                Toast.makeText(requireContext(), "请先登录后再收藏", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentStatus = viewModel.isFavorited.value ?: false
            viewModel.toggleFavorite(auth, workId)
            Toast.makeText(
                requireContext(),
                if (currentStatus) "已取消收藏" else "已收藏",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.authorButton.setOnClickListener {
            findNavController().navigate(R.id.action_poemDetail_to_authorDetail)
        }
    }

    override fun onResume() {
        super.onResume()
        val workId = arguments?.getLong("workId", 0L) ?: 0L
        if (workId > 0) {
            viewModel.checkFavoriteStatus(SessionManager(requireContext()).bearerAuthorization(), workId)
        }
    }

    private fun formatPoemContent(content: String): String {
        // 先按换行符或多个空格分割成段落
        val paragraphs = content
            .split(Regex("[\n]+|[ ]{2,}"))  // 按换行符或连续2个以上空格分割
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // 如果有2个及以上的段落，说明是词/赋，按段落换行
        return if (paragraphs.size >= 2) {
            // 词/赋：保留段落结构
            paragraphs.joinToString("\n\n")
        } else {
            // 诗：去掉所有空格和换行，只在句号后换行
            content
                .replace("\n", "")
                .replace(" ", "")
                .replace("。", "。\n")
                .trim()
        }
    }

    private fun setupStickyContentCard() {
        binding.mainScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            // 获取原文TextView的位置
            val contentTextLocation = IntArray(2)
            binding.contentText.getLocationOnScreen(contentTextLocation)
            val contentTextTop = contentTextLocation[1]

            // 获取原文TextView的底部位置
            val contentTextBottom = contentTextTop + binding.contentText.height

            // 如果原文的底部已经滚出屏幕顶部，显示固定窗口
            if (contentTextBottom < 200) {
                binding.stickyContentCard.visibility = View.VISIBLE
            } else {
                binding.stickyContentCard.visibility = View.GONE
            }
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
