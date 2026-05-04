package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
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
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(14)))
            isNestedScrollingEnabled = false
        }

        viewModel.quickActions.observe(viewLifecycleOwner) {
            actionAdapter.submitList(it)
        }
        viewModel.homeProgressStats.observe(viewLifecycleOwner) { stats ->
            if (stats.size >= 2) {
                binding.statLearnedValue.text = stats[0].value
                binding.statCheckInValue.text = stats[1].value
            }
        }

        binding.loginHintButton.setOnClickListener {
            findNavController().navigate(R.id.action_user_to_login)
        }
    }

    override fun onResume() {
        super.onResume()
        bindProfileHeader()
    }

    private fun bindProfileHeader() {
        val session = SessionManager(requireContext())
        val loggedIn = session.isLoggedIn()
        binding.loggedInPanel.isVisible = loggedIn
        binding.guestPanel.isVisible = !loggedIn
        binding.loggedInOnlySection.isVisible = loggedIn
        if (loggedIn) {
            val nick = session.nickname()?.trim().orEmpty().ifBlank { session.username().orEmpty() }
            val account = session.username().orEmpty()
            binding.userNicknameTitle.text = nick
            binding.userAccountSubtitle.text =
                if (account.isNotEmpty()) getString(R.string.user_center_account_fmt, account) else ""
            val initial = nick.firstOrNull()?.uppercaseChar()
                ?: account.firstOrNull()?.uppercaseChar()
                ?: '?'
            binding.avatarInitial.text = initial.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
