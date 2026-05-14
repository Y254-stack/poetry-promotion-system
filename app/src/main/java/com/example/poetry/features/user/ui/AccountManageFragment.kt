package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.auth.SavedLoginAccount
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.databinding.FragmentAccountManageBinding
import com.example.poetry.features.user.adapter.SavedAccountsAdapter

class AccountManageFragment : Fragment(R.layout.fragment_account_manage) {

    private var _binding: FragmentAccountManageBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private val listAdapter = SavedAccountsAdapter(
        onSwitch = { account -> switchTo(account) },
        onRemove = { account -> confirmRemove(account) }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAccountManageBinding.bind(view)
        sessionManager = SessionManager(requireContext())
        binding.accountRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.accountRecycler.adapter = listAdapter
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val currentId = sessionManager.userId()
        val list = sessionManager.listSavedLoginAccounts()
        binding.emptyHint.isVisible = list.isEmpty()
        listAdapter.submit(list, currentId)
    }

    private fun switchTo(account: SavedLoginAccount) {
        if (!sessionManager.switchToAccount(account.userId)) {
            Toast.makeText(requireContext(), "切换失败", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(requireContext(), getString(R.string.account_manage_switched), Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    private fun confirmRemove(account: SavedLoginAccount) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.account_manage_remove)
            .setMessage("将从本机移除该账号的免密登录信息，不影响服务器上的账号数据。")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                sessionManager.removeSavedAccount(account.userId)
                refreshList()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
