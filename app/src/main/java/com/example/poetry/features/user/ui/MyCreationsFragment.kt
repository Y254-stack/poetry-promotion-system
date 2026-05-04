package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentMyCreationsBinding
import com.example.poetry.features.user.adapter.CreationListAdapter
import com.example.poetry.features.user.model.UserCollectionUiModel
import com.example.poetry.features.user.viewmodel.UserViewModel
import com.google.android.material.tabs.TabLayout

class MyCreationsFragment : Fragment(R.layout.fragment_my_creations) {

    private var _binding: FragmentMyCreationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: CreationListAdapter
    private var showingDrafts = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyCreationsBinding.bind(view)

        adapter = CreationListAdapter(
            onOpen = {
                Toast.makeText(requireContext(), "编辑与预览（待接入）", Toast.LENGTH_SHORT).show()
            },
            onDelete = { item -> confirmDelete(item) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyCreationsFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
        }

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.creation_tab_published))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.creation_tab_drafts))
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showingDrafts = tab?.position == 1
                refreshAdapter()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        viewModel.publishedCreations.observe(viewLifecycleOwner) {
            if (!showingDrafts) adapter.submitList(it)
        }
        viewModel.draftCreations.observe(viewLifecycleOwner) {
            if (showingDrafts) adapter.submitList(it)
        }

        binding.fabAdd.setOnClickListener {
            viewModel.addDraftPlaceholder("未命名创作 ${System.currentTimeMillis() % 1000}")
            Toast.makeText(requireContext(), "已加入草稿箱（演示）", Toast.LENGTH_SHORT).show()
            binding.tabLayout.getTabAt(1)?.select()
        }

        refreshAdapter()
    }

    private fun refreshAdapter() {
        val list = if (showingDrafts) {
            viewModel.draftCreations.value.orEmpty()
        } else {
            viewModel.publishedCreations.value.orEmpty()
        }
        adapter.submitList(list)
    }

    private fun confirmDelete(item: UserCollectionUiModel) {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.creation_delete_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (showingDrafts) viewModel.deleteDraft(item) else viewModel.deletePublished(item)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
