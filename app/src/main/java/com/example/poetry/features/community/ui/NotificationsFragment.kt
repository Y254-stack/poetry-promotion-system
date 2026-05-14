package com.example.poetry.features.community.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentNotificationsBinding
import com.example.poetry.features.community.adapter.NotificationAdapter
import com.example.poetry.features.community.model.NotificationUiModel
import com.example.poetry.features.community.repository.CommunityRepositoryImpl
import com.example.poetry.features.community.repository.FollowRepositoryImpl
import com.example.poetry.features.community.viewmodel.CommunityViewModel
import com.example.poetry.core.auth.SessionManager

class NotificationsFragment : Fragment(R.layout.fragment_notifications) {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommunityViewModel
    private lateinit var adapter: NotificationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotificationsBinding.bind(view)

        val sessionManager = SessionManager(requireContext())
        val communityRepository = CommunityRepositoryImpl(
            com.example.poetry.core.network.NetworkModule.poetryApiService,
            sessionManager
        )
        val followRepository = FollowRepositoryImpl(
            com.example.poetry.core.network.NetworkModule.poetryApiService,
            sessionManager
        )
        viewModel = CommunityViewModel(communityRepository, followRepository)

        setupRecyclerView()
        setupClickListeners()
        observeData()
        
        viewModel.loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            onItemClick = { notification ->
                handleNotificationClick(notification)
            },
            onDeleteClick = { notification ->
                viewModel.deleteNotification(notification.notificationId)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NotificationsFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(10)))
        }
    }

    private fun setupClickListeners() {
        binding.tvMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
        }

        binding.tvDeleteAll.setOnClickListener {
            viewModel.deleteAllNotifications()
        }
    }

    private fun observeData() {
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            adapter.submitList(notifications)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleNotificationClick(notification: NotificationUiModel) {
        if (!notification.isRead) {
            viewModel.markAsRead(notification.notificationId)
        }

        val bundle = Bundle().apply {
            putLong("postId", notification.postId)
        }
        findNavController().navigate(R.id.action_notifications_to_postDetail, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
