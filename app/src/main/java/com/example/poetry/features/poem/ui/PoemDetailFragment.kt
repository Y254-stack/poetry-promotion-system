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
import com.example.poetry.features.poem.model.PoemDetailUiModel
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
        if (workId > 0L) {
            viewModel.loadPoemDetail(workId)
            viewModel.loadRelatedWorks(workId)
            viewModel.checkFavoriteStatus(SessionManager(requireContext()).bearerAuthorization(), workId)
        }

        viewModel.poemDetail.observe(viewLifecycleOwner) { detail ->
            bindPoemDetail(detail)
        }

        viewModel.relatedWorks.observe(viewLifecycleOwner) {
            relatedAdapter.submitList(it)
        }

        viewModel.isFavorited.observe(viewLifecycleOwner) { isFavorited ->
            updateFavoriteButton(isFavorited)
        }

        binding.favoriteButton.setOnClickListener {
            if (workId <= 0L) return@setOnClickListener
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
            val detail = viewModel.poemDetail.value
            if (detail == null || detail.authorId <= 0L) {
                Toast.makeText(requireContext(), "暂无作者信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            findNavController().navigate(
                R.id.action_poemDetail_to_authorDetail,
                bundleOf(
                    "authorId" to detail.authorId,
                    "authorName" to detail.author,
                    "dynastyName" to detail.dynasty
                )
            )
        }

        binding.createAppreciationButton.setOnClickListener {
            val detail = viewModel.poemDetail.value ?: return@setOnClickListener
            findNavController().navigate(
                R.id.createPostFragment,
                bundleOf(
                    "prefill_title" to "《${detail.title}》赏析",
                    "prefill_content" to buildAppreciationTemplate(detail),
                    "prefill_tag" to "诗词赏析",
                    "source_work_id" to detail.workId,
                    "source_author_id" to detail.authorId
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val workId = arguments?.getLong("workId", 0L) ?: 0L
        if (workId > 0L) {
            viewModel.checkFavoriteStatus(SessionManager(requireContext()).bearerAuthorization(), workId)
        }
    }

    private fun bindPoemDetail(detail: PoemDetailUiModel) {
        binding.titleText.text = detail.title
        binding.authorButton.text = "${detail.author} · ${detail.dynasty}"

        val formattedContent = formatPoemContent(detail.title, detail.content)
        binding.contentText.text = formattedContent
        binding.stickyContentText.text = formattedContent

        binding.translationText.text = detail.translation.ifBlank { "暂无译文" }
        binding.annotationText.text = detail.annotation.ifBlank { "暂无注释" }
        binding.appreciationText.text = detail.appreciation.ifBlank { "暂无赏析" }
    }

    private fun buildAppreciationTemplate(detail: PoemDetailUiModel): String {
        val formattedContent = formatPoemContent(detail.title, detail.content)
        return buildString {
            append("作品：")
            append(detail.title)
            append('\n')
            append("作者：")
            append(detail.author)
            append("（")
            append(detail.dynasty)
            append("）")
            append("\n\n")
            append("原文：\n")
            append(formattedContent)
            append("\n\n")
            append("我的赏析：\n")
            append("1. 这首作品最打动我的意象是：\n")
            append("2. 我理解到的情感和主题是：\n")
            append("3. 我最喜欢的句子及原因：")
        }
    }

    private fun formatPoemContent(title: String, content: String): String {
        val normalized = content
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace("\u3000", "")
            .replace("¤", "\n\n")
            .trim()

        if (normalized.isBlank()) return ""

        val rawBlocks = normalized
            .split(Regex("\n\\s*\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val blocks = if (rawBlocks.isNotEmpty()) rawBlocks else listOf(normalized)

        if (isCiLike(title)) {
            return formatCiContent(blocks)
        }

        val mergedVerse = mergeVerseBlocks(blocks)
        if (mergedVerse != null) {
            return formatVerseBlock(mergedVerse)
        }

        return blocks.joinToString("\n\n") { block -> formatProseBlock(block) }
    }

    private fun formatCiContent(blocks: List<String>): String {
        if (blocks.size >= 2) {
            return blocks.joinToString("\n\n") { block ->
                block.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .joinToString("")
            }
        }

        val compact = blocks.joinToString("") { it.replace(Regex("\\s+"), "") }
        if (compact.isBlank()) return ""
        val segments = splitSentences(compact)
        if (segments.isEmpty()) return compact

        return splitCiStanzas(segments)
            .map { stanza -> stanza.joinToString("") }
            .joinToString("\n\n")
    }

    private fun mergeVerseBlocks(blocks: List<String>): String? {
        val normalizedBlocks = blocks.map { block ->
            block.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString("")
        }.filter { it.isNotBlank() }

        if (normalizedBlocks.isEmpty()) return null
        val allSegments = normalizedBlocks.flatMap { splitSentences(it) }
        return if (looksLikeVerseSegments(allSegments)) {
            normalizedBlocks.joinToString("")
        } else {
            null
        }
    }

    private fun formatVerseBlock(block: String): String {
        val segments = splitSentences(block.replace(Regex("\\s+"), ""))
        if (segments.isEmpty()) return block.trim()
        return segments.chunked(2).joinToString("\n") { pair ->
            pair.joinToString("")
        }
    }

    private fun formatProseBlock(block: String): String {
        val compact = block.replace(Regex("\\s+"), "")
        if (compact.isBlank()) return ""
        val segments = splitSentences(compact)
        if (segments.isEmpty()) return compact
        return formatSentenceFlow(segments)
    }

    private fun splitCiStanzas(segments: List<String>): List<List<String>> {
        if (segments.size <= 4) return listOf(segments)
        if (segments.size == 6) {
            return listOf(segments.subList(0, 3), segments.subList(3, 6))
        }
        if (segments.size == 8) {
            return listOf(segments.subList(0, 4), segments.subList(4, 8))
        }

        val half = segments.size / 2
        val splitIndex = when {
            segments.size % 2 == 0 -> half
            else -> half + 1
        }.coerceIn(2, segments.size - 2)

        return listOf(
            segments.subList(0, splitIndex),
            segments.subList(splitIndex, segments.size)
        )
    }

    private fun formatSentenceFlow(segments: List<String>): String {
        if (segments.isEmpty()) return ""

        val units = segments.flatMap { splitLongSentence(it) }
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        units.forEach { unit ->
            val piece = unit.trim()
            if (piece.isBlank()) return@forEach

            val projectedLength = visibleLength(currentLine.toString()) + visibleLength(piece)
            if (currentLine.isNotEmpty() && projectedLength > 24) {
                lines += currentLine.toString()
                currentLine = StringBuilder()
            }

            currentLine.append(piece)

            val lastChar = piece.lastOrNull()
            val shouldBreak = lastChar in setOf('。', '！', '？', '；') ||
                visibleLength(currentLine.toString()) >= 26

            if (shouldBreak) {
                lines += currentLine.toString()
                currentLine = StringBuilder()
            }
        }

        if (currentLine.isNotEmpty()) {
            lines += currentLine.toString()
        }

        return lines.joinToString("\n")
    }

    private fun splitLongSentence(sentence: String): List<String> {
        if (visibleLength(sentence) <= 18) return listOf(sentence)

        val byPause = splitByPause(sentence)
        if (byPause.size > 1) return byPause

        return hardWrap(sentence, 18)
    }

    private fun splitByPause(sentence: String): List<String> {
        val pauseMarks = setOf('、', '：')
        val result = mutableListOf<String>()
        val current = StringBuilder()

        sentence.forEach { ch ->
            current.append(ch)
            if (ch in pauseMarks) {
                result += current.toString().trim()
                current.clear()
            }
        }

        if (current.isNotEmpty()) {
            result += current.toString().trim()
        }

        return result.filter { it.isNotBlank() }
    }

    private fun hardWrap(text: String, maxLength: Int): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()

        text.forEach { ch ->
            current.append(ch)
            if (visibleLength(current.toString()) >= maxLength) {
                result += current.toString()
                current = StringBuilder()
            }
        }

        if (current.isNotEmpty()) {
            result += current.toString()
        }

        return result
    }

    private fun visibleLength(text: String): Int {
        return text.count { ch ->
            !ch.isWhitespace() && ch !in setOf('，', '。', '！', '？', '；', '：', '、')
        }
    }

    private fun splitSentences(text: String): List<String> {
        val punctuation = setOf('，', '。', '！', '？', '；', '：')
        val result = mutableListOf<String>()
        val current = StringBuilder()

        text.forEach { ch ->
            current.append(ch)
            if (ch in punctuation) {
                result += current.toString().trim()
                current.clear()
            }
        }

        if (current.isNotEmpty()) {
            result += current.toString().trim()
        }

        return result.filter { it.isNotBlank() }
    }

    private fun looksLikeVerseSegments(segments: List<String>): Boolean {
        if (segments.size < 2) return false
        val lengths = segments.map { visibleLength(it) }
        val maxLength = lengths.maxOrNull() ?: 0
        val averageLength = lengths.average()
        return maxLength <= 14 && averageLength <= 10.5
    }

    private fun isCiLike(title: String): Boolean {
        return title.contains('·') || title.contains('・') || title.contains('•')
    }

    private fun setupStickyContentCard() {
        binding.mainScrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            val contentTextLocation = IntArray(2)
            binding.contentText.getLocationOnScreen(contentTextLocation)
            val contentTextTop = contentTextLocation[1]
            val contentTextBottom = contentTextTop + binding.contentText.height

            binding.stickyContentCard.visibility = if (contentTextBottom < 200) {
                View.VISIBLE
            } else {
                View.GONE
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
