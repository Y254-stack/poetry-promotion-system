package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        refreshNicknamePreview()

        binding.nicknameButton.setOnClickListener {
            val session = SessionManager(requireContext())
            val input = EditText(requireContext()).apply {
                setText(session.nickname().orEmpty())
                hint = "新昵称"
            }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_change_nickname)
                .setView(input)
                .setPositiveButton("保存") { _, _ ->
                    val text = input.text?.toString()?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        session.saveSession(
                            session.token().orEmpty(),
                            session.userId(),
                            session.username().orEmpty(),
                            text
                        )
                        refreshNicknamePreview()
                        Toast.makeText(requireContext(), "昵称已更新（本地）", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.avatarButton.setOnClickListener {
            Toast.makeText(requireContext(), "头像将从相册选择（待接入）", Toast.LENGTH_SHORT).show()
        }

        binding.passwordButton.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_changePassword)
        }

        binding.logoutButton.setOnClickListener {
            SessionManager(requireContext()).clearActiveSession()
            Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        binding.accountManageButton.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_accountManage)
        }

        binding.deleteAccountButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_delete_account)
                .setMessage("注销后将清除本地会话，后端流程接入前仅为演示。")
                .setPositiveButton("确认注销") { _, _ ->
                    SessionManager(requireContext()).clearEverything()
                    Toast.makeText(requireContext(), "已清除本地登录状态", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun refreshNicknamePreview() {
        val session = SessionManager(requireContext())
        val nick = session.nickname()?.trim().orEmpty().ifBlank { session.username().orEmpty() }
        binding.nicknamePreview.text = nick.ifEmpty { "未设置" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
