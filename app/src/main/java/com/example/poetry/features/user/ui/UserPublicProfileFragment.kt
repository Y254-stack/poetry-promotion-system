package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.poetry.R
import com.example.poetry.databinding.FragmentUserPublicProfileBinding

class UserPublicProfileFragment : Fragment(R.layout.fragment_user_public_profile) {

    private var _binding: FragmentUserPublicProfileBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUserPublicProfileBinding.bind(view)

        val userId = arguments?.getString("userId").orEmpty()
        val displayName = arguments?.getString("displayName").orEmpty()
        binding.nameText.text = displayName.ifEmpty { "用户" }
        binding.idText.text = if (userId.isNotEmpty()) "ID：$userId" else ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
