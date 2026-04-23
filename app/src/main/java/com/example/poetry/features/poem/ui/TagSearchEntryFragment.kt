package com.example.poetry.features.poem.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.poetry.R
import com.example.poetry.databinding.FragmentTagSearchEntryBinding
import com.example.poetry.features.poem.model.TagUiModel
import com.example.poetry.features.poem.viewmodel.PoemViewModel
import com.google.android.material.chip.Chip

class TagSearchEntryFragment : Fragment(R.layout.fragment_tag_search_entry) {

    private var _binding: FragmentTagSearchEntryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PoemViewModel by viewModels()
    private var filterPanelVisible = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTagSearchEntryBinding.bind(view)

        binding.tagToggleButton.setOnClickListener {
            filterPanelVisible = !filterPanelVisible
            binding.tagPanel.visibility = if (filterPanelVisible) View.VISIBLE else View.GONE
            binding.tagToggleButton.text = if (filterPanelVisible) "收起标签" else "标签筛选"
        }

        binding.searchButton.setOnClickListener {
            val state = viewModel.searchUiState.value
            val selectedIds = state?.selectedTagIds.orEmpty()
            if (selectedIds.isEmpty()) {
                Toast.makeText(requireContext(), "请至少选择一个标签", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedTags = state?.availableTags
                .orEmpty()
                .filter { it.tagId in selectedIds }
                .map { it.tagName }

            findNavController().navigate(
                R.id.searchResultFragment,
                bundleOf(
                    "selectedTagIds" to selectedIds.toLongArray(),
                    "selectedTagNames" to ArrayList(selectedTags)
                )
            )
        }

        viewModel.searchUiState.observe(viewLifecycleOwner) { state ->
            binding.tagErrorText.visibility =
                if (state.errorMessage.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.tagErrorText.text = state.errorMessage
            renderTags(state.availableTags, state.selectedTagIds)
        }

        viewModel.loadHotTags()
    }

    private fun renderTags(tags: List<TagUiModel>, selectedTagIds: List<Long>) {
        binding.tagChipGroup.removeAllViews()
        tags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag.tagName
                isCheckable = true
                isChecked = selectedTagIds.contains(tag.tagId)
                setOnClickListener {
                    viewModel.toggleTagSelectionOnly(tag.tagId)
                }
            }
            binding.tagChipGroup.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
