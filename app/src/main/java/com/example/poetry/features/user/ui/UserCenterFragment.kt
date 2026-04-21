package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentUserCenterBinding
import com.example.poetry.features.user.adapter.UserQuickActionAdapter
import com.example.poetry.features.user.viewmodel.UserViewModel

class UserCenterFragment : Fragment(R.layout.fragment_user_center) {

    private var _binding: FragmentUserCenterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()
    private lateinit var actionAdapter: UserQuickActionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUserCenterBinding.bind(view)

        actionAdapter = UserQuickActionAdapter {
            findNavController().navigate(it.destinationId)
        }
        binding.actionRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = actionAdapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(10)))
            isNestedScrollingEnabled = false
        }

        viewModel.quickActions.observe(viewLifecycleOwner) {
            actionAdapter.submitList(it)
        }
        viewModel.progressStats.observe(viewLifecycleOwner) { stats ->
            if (stats.size >= 4) {
                binding.statOneValue.text = stats[0].value
                binding.statTwoValue.text = stats[1].value
                binding.statThreeValue.text = stats[2].value
                binding.statFourValue.text = stats[3].value
            }
        }

        binding.loginHintButton.setOnClickListener {
            findNavController().navigate(R.id.action_user_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
