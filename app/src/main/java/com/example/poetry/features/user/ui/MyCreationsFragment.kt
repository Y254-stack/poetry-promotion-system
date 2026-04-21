package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentMyCreationsBinding
import com.example.poetry.features.user.adapter.UserCollectionAdapter
import com.example.poetry.features.user.viewmodel.UserViewModel

class MyCreationsFragment : Fragment(R.layout.fragment_my_creations) {

    private var _binding: FragmentMyCreationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: UserCollectionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyCreationsBinding.bind(view)

        adapter = UserCollectionAdapter {
            // TODO: 跳转卡片详情或编辑页
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyCreationsFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
        }

        viewModel.creations.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
