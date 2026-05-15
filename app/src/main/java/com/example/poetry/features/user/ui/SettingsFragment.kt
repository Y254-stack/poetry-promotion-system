package com.example.poetry.features.user.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.network.BackendUrl
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.databinding.FragmentSettingsBinding
import kotlin.concurrent.thread
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val pickAvatar = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadAvatar(uri)
        }
    }

    /** 模拟器经 Device Explorer 拷入的图片可能未进 MediaStore，用文件选择器可直达 Download 等目录。 */
    private val pickAvatarFromFiles = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadAvatar(uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        refreshNicknamePreview()
        refreshAvatarPreview()

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
                            text,
                            session.avatarUrl()
                        )
                        refreshNicknamePreview()
                        Toast.makeText(requireContext(), "昵称已更新（本地）", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.avatarButton.setOnClickListener {
            val session = SessionManager(requireContext())
            if (!session.isLoggedIn()) {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_change_avatar)
                .setItems(arrayOf("系统相册", "从文件夹选择（模拟器推荐）")) { _, which ->
                    when (which) {
                        0 -> pickAvatar.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                        1 -> pickAvatarFromFiles.launch("image/*")
                    }
                }
                .show()
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

    override fun onResume() {
        super.onResume()
        refreshAvatarPreview()
    }

    private fun refreshNicknamePreview() {
        val session = SessionManager(requireContext())
        val nick = session.nickname()?.trim().orEmpty().ifBlank { session.username().orEmpty() }
        binding.nicknamePreview.text = nick.ifEmpty { "未设置" }
    }

    private fun refreshAvatarPreview() {
        val session = SessionManager(requireContext())
        val abs = BackendUrl.toAbsolute(session.avatarUrl())
        if (abs != null) {
            binding.settingsAvatarPreview.isVisible = true
            binding.settingsAvatarPreview.load(abs) {
                transformations(CircleCropTransformation())
                crossfade(true)
            }
        } else {
            binding.settingsAvatarPreview.isVisible = false
            binding.settingsAvatarPreview.setImageDrawable(null)
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val session = SessionManager(requireContext())
        val auth = session.bearerAuthorization()
        if (auth == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }
        val cr = requireContext().contentResolver
        val mime = cr.getType(uri) ?: "image/jpeg"
        val bytes = try {
            cr.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
        if (bytes == null || bytes.isEmpty()) {
            Toast.makeText(requireContext(), "无法读取图片", Toast.LENGTH_SHORT).show()
            return
        }
        val ext = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "avatar.$ext", body)
        Toast.makeText(requireContext(), "正在上传…", Toast.LENGTH_SHORT).show()
        thread {
            try {
                val response = NetworkModule.poetryApiService.uploadMyAvatar(auth, part).execute()
                activity?.runOnUiThread {
                    if (response.isSuccessful) {
                        val url = response.body()?.avatarUrl
                        if (url != null) {
                            session.saveSession(
                                session.token().orEmpty(),
                                session.userId(),
                                session.username().orEmpty(),
                                session.nickname().orEmpty(),
                                url
                            )
                            refreshAvatarPreview()
                            Toast.makeText(requireContext(), "头像已更新", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "上传失败：无返回数据", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "上传失败 (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "上传失败: ${e.message ?: "网络错误"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
