package com.example.poetry.features.auth.ui

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.poetry.R
import com.example.poetry.databinding.FragmentRegisterBinding
import com.example.poetry.features.auth.viewmodel.AuthViewModel

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        bindAgreementText()

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.registerButton.isEnabled = !loading
            binding.registerButton.text = if (loading) "注册中..." else getString(R.string.action_register)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.authResult.observe(viewLifecycleOwner) { auth ->
            auth?.let {
                val session = com.example.poetry.core.auth.SessionManager(requireContext())
                session.saveSession(it.token, it.userId, it.username, it.nickname)
                val nav = findNavController()
                // 栈一般为：个人中心 → 登录 → 注册； inclusive 弹出登录与注册，回到个人中心
                val poppedToUserCenter = nav.popBackStack(R.id.loginFragment, true)
                if (!poppedToUserCenter) {
                    nav.popBackStack()
                }
                val appCtx = requireContext().applicationContext
                Toast.makeText(appCtx, appCtx.getString(R.string.toast_register_auto_login), Toast.LENGTH_LONG).show()
            }
        }

        binding.registerButton.setOnClickListener {
            val username = binding.accountEdit.text?.toString().orEmpty()
            val nickname = binding.nicknameEdit.text?.toString().orEmpty()
            val email = binding.emailEdit.text?.toString().orEmpty()
            val password = binding.passwordEdit.text?.toString().orEmpty()
            val confirm = binding.confirmEdit.text?.toString().orEmpty()
            viewModel.register(
                username = username,
                nickname = nickname,
                email = email,
                password = password,
                confirm = confirm,
                agreedToTerms = binding.agreeTermsCheckBox.isChecked
            )
        }

        binding.goLoginText.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun bindAgreementText() {
        val full = "我已阅读并同意《用户协议》和《隐私政策》"
        val spannable = SpannableString(full)
        val linkColor = ContextCompat.getColor(requireContext(), R.color.user_center_primary)
        val uaStart = full.indexOf("《用户协议》")
        val uaEnd = uaStart + "《用户协议》".length
        val pvStart = full.indexOf("《隐私政策》")
        val pvEnd = pvStart + "《隐私政策》".length

        val showUserAgreement = {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.legal_user_agreement_title)
                .setMessage(R.string.legal_placeholder_body)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        val showPrivacy = {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.legal_privacy_title)
                .setMessage(R.string.legal_placeholder_body)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    showUserAgreement()
                }
            },
            uaStart,
            uaEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(ForegroundColorSpan(linkColor), uaStart, uaEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    showPrivacy()
                }
            },
            pvStart,
            pvEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(ForegroundColorSpan(linkColor), pvStart, pvEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.agreeTermsText.text = spannable
        binding.agreeTermsText.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
