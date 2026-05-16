package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.network.BackendUrl
import com.example.poetry.databinding.FragmentUserCenterBinding
import com.example.poetry.features.user.model.UserQuickActionUiModel
import com.example.poetry.features.user.viewmodel.UserViewModel

class UserCenterFragment : Fragment(R.layout.fragment_user_center) {

    private var _binding: FragmentUserCenterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUserCenterBinding.bind(view)

        viewModel.quickActions.observe(viewLifecycleOwner, ::bindQuickActions)
        viewModel.homeProgressStats.observe(viewLifecycleOwner) { stats ->
            if (stats.isNotEmpty()) {
                binding.statLearnedInline.text = "${stats.first().title} ${stats.first().value}"
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

        if (!loggedIn) {
            binding.root.post {
                if (findNavController().currentDestination?.id == R.id.userCenterFragment) {
                    findNavController().navigate(R.id.action_user_to_login)
                }
            }
            return
        }

        val nick = session.nickname()?.trim().orEmpty().ifBlank { session.username().orEmpty() }
        val account = session.username().orEmpty()
        binding.userNicknameTitle.text = nick
        binding.userAccountSubtitle.text =
            if (account.isNotEmpty()) getString(R.string.user_center_account_fmt, account) else ""

        val initial = nick.firstOrNull()?.uppercaseChar()
            ?: account.firstOrNull()?.uppercaseChar()
            ?: '?'
        binding.avatarInitial.text = initial.toString()

        val avatarAbs = BackendUrl.toAbsolute(session.avatarUrl())
        if (avatarAbs != null) {
            binding.avatarImage.isVisible = true
            binding.avatarInitial.isVisible = false
            binding.avatarImage.load(avatarAbs) {
                transformations(CircleCropTransformation())
                crossfade(true)
            }
        } else {
            binding.avatarImage.isVisible = false
            binding.avatarImage.setImageDrawable(null)
            binding.avatarInitial.isVisible = true
        }
    }

    private fun bindQuickActions(actions: List<UserQuickActionUiModel>) {
        val compactBindings = listOf(
            CompactActionViews(binding.actionCard1, binding.actionBadge1, binding.actionTitle1, binding.actionSubtitle1),
            CompactActionViews(binding.actionCard2, binding.actionBadge2, binding.actionTitle2, binding.actionSubtitle2),
            CompactActionViews(binding.actionCard3, binding.actionBadge3, binding.actionTitle3, binding.actionSubtitle3),
            CompactActionViews(binding.actionCard4, binding.actionBadge4, binding.actionTitle4, binding.actionSubtitle4)
        )

        compactBindings.forEachIndexed { index, views ->
            val action = actions.getOrNull(index)
            if (action == null) {
                views.card.visibility = View.INVISIBLE
            } else {
                views.card.visibility = View.VISIBLE
                views.badge.text = action.title.take(1)
                views.title.text = action.title
                views.subtitle.text = action.subtitle
                views.card.setOnClickListener { findNavController().navigate(action.destinationId) }
            }
        }

        val followAction = actions.getOrNull(4)
        binding.actionFollowCard.isVisible = followAction != null
        if (followAction != null) {
            binding.actionBadge5.text = followAction.title.take(1)
            binding.actionTitle5.text = followAction.title
            binding.actionSubtitle5.text = followAction.subtitle
            binding.actionFollowCard.setOnClickListener {
                findNavController().navigate(followAction.destinationId)
            }
        }
    }

    private data class CompactActionViews(
        val card: View,
        val badge: android.widget.TextView,
        val title: android.widget.TextView,
        val subtitle: android.widget.TextView
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
