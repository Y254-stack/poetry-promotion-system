package com.example.poetry.features.auth.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.poetry.core.network.ApiAuthResponse
import com.example.poetry.core.network.ApiForgotPasswordSendCodeResponse
import com.example.poetry.features.auth.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var repository: AuthRepository
    private lateinit var viewModel: AuthViewModel

    private val successAuth = ApiAuthResponse(
        token = "jwt-token",
        userId = 1L,
        username = "tester",
        nickname = "测试用户",
        avatarUrl = null
    )

    @Before
    fun setUp() {
        repository = mock()
        viewModel = AuthViewModel(repository)
    }

    // --- login validation ---

    @Test
    fun login_blankAccount_setsValidationError() {
        viewModel.login("", "123456")

        assertEquals("请输入账号或邮箱", viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value!!)
        verify(repository, never()).login(any(), any())
    }

    @Test
    fun login_shortPassword_setsValidationError() {
        viewModel.login("user@example.com", "12345")

        assertEquals("密码长度至少 6 位", viewModel.errorMessage.value)
        verify(repository, never()).login(any(), any())
    }

    @Test
    fun login_success_setsAuthResult() {
        val call = mockCall<ApiAuthResponse>()
        whenever(repository.login("user@example.com", "123456")).thenReturn(call)
        enqueueSuccess(call, successAuth)

        viewModel.login("user@example.com", "123456")

        assertEquals(successAuth, viewModel.authResult.value)
        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value!!)
    }

    @Test
    fun login_unauthorized_parsesServerMessage() {
        val call = mockCall<ApiAuthResponse>()
        whenever(repository.login(any(), any())).thenReturn(call)
        val errorBody = """{"message":"密码错误，还可尝试 4 次"}"""
            .toResponseBody("application/json".toMediaType())
        enqueueError(call, 401, errorBody)

        viewModel.login("user@example.com", "123456")

        assertEquals("密码错误，还可尝试 4 次", viewModel.errorMessage.value)
        assertNull(viewModel.authResult.value)
    }

    @Test
    fun login_lockedAccount_showsServerMessage() {
        val call = mockCall<ApiAuthResponse>()
        whenever(repository.login(any(), any())).thenReturn(call)
        val errorBody = """{"message":"因连续登录失败，账号已暂时锁定，请稍后再试"}"""
            .toResponseBody("application/json".toMediaType())
        enqueueError(call, 423, errorBody)

        viewModel.login("user@example.com", "wrong-pass")

        assertEquals("因连续登录失败，账号已暂时锁定，请稍后再试", viewModel.errorMessage.value)
    }

    @Test
    fun login_networkFailure_setsError() {
        val call = mockCall<ApiAuthResponse>()
        whenever(repository.login(any(), any())).thenReturn(call)
        whenever(call.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<ApiAuthResponse>
            callback.onFailure(call, RuntimeException("timeout"))
            null
        }

        viewModel.login("user@example.com", "123456")

        assertEquals("timeout", viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value!!)
    }

    // --- register validation ---

    @Test
    fun register_termsNotAgreed_setsValidationError() {
        viewModel.register(
            username = "newuser",
            nickname = "昵称",
            email = "user@example.com",
            password = "123456",
            confirm = "123456",
            agreedToTerms = false
        )

        assertEquals("请先阅读并同意《用户协议》和《隐私政策》", viewModel.errorMessage.value)
        verify(repository, never()).register(any(), any(), any(), any(), any())
    }

    @Test
    fun register_usernameTooShort_setsValidationError() {
        viewModel.register(
            username = "ab",
            nickname = "昵称",
            email = "user@example.com",
            password = "123456",
            confirm = "123456",
            agreedToTerms = true
        )

        assertEquals("账号长度至少 3 位", viewModel.errorMessage.value)
    }

    @Test
    fun register_invalidEmail_setsValidationError() {
        viewModel.register(
            username = "newuser",
            nickname = "昵称",
            email = "not-an-email",
            password = "123456",
            confirm = "123456",
            agreedToTerms = true
        )

        assertEquals("请输入有效的邮箱地址", viewModel.errorMessage.value)
    }

    @Test
    fun register_passwordMismatch_setsValidationError() {
        viewModel.register(
            username = "newuser",
            nickname = "昵称",
            email = "user@example.com",
            password = "123456",
            confirm = "654321",
            agreedToTerms = true
        )

        assertEquals("两次输入的密码不一致", viewModel.errorMessage.value)
    }

    @Test
    fun register_success_setsAuthResult() {
        val call = mockCall<ApiAuthResponse>()
        whenever(
            repository.register(
                username = "newuser",
                nickname = "昵称",
                email = "user@example.com",
                password = "123456",
                agreedToTerms = true
            )
        ).thenReturn(call)
        enqueueSuccess(call, successAuth)

        viewModel.register(
            username = "newuser",
            nickname = "昵称",
            email = "user@example.com",
            password = "123456",
            confirm = "123456",
            agreedToTerms = true
        )

        assertEquals(successAuth, viewModel.authResult.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun register_conflict_showsServerMessage() {
        val call = mockCall<ApiAuthResponse>()
        whenever(repository.register(any(), any(), any(), any(), any())).thenReturn(call)
        val errorBody = """{"message":"用户名已存在"}"""
            .toResponseBody("application/json".toMediaType())
        enqueueError(call, 409, errorBody)

        viewModel.register(
            username = "taken",
            nickname = "昵称",
            email = "user@example.com",
            password = "123456",
            confirm = "123456",
            agreedToTerms = true
        )

        assertEquals("用户名已存在", viewModel.errorMessage.value)
    }

    // --- forgot password ---

    @Test
    fun sendVerificationCode_invalidEmail_setsError() = runTest {
        viewModel.sendVerificationCode("bad-email")
        mainCoroutineRule.advanceUntilIdle()

        assertEquals("请输入有效的邮箱地址", viewModel.sendCodeError.value)
        assertFalse(viewModel.codeSent.value!!)
    }

    @Test
    fun sendVerificationCode_success_setsCodeSent() = runTest {
        whenever(repository.sendVerificationCode("user@example.com"))
            .thenReturn(ApiForgotPasswordSendCodeResponse(message = "ok"))

        viewModel.sendVerificationCode("user@example.com")
        mainCoroutineRule.advanceUntilIdle()

        assertTrue(viewModel.codeSent.value!!)
        assertNull(viewModel.sendCodeError.value)
        assertFalse(viewModel.isLoading.value!!)
    }

    @Test
    fun resetPassword_invalidEmail_setsError() = runTest {
        viewModel.resetPassword(
            email = "invalid",
            verificationCode = "123456",
            newPassword = "123456",
            confirmPassword = "123456"
        )
        mainCoroutineRule.advanceUntilIdle()

        assertEquals("请输入有效的邮箱地址", viewModel.resetError.value)
        assertFalse(viewModel.resetSuccess.value!!)
    }

    @Test
    fun resetPassword_blankCode_setsError() = runTest {
        viewModel.resetPassword(
            email = "user@example.com",
            verificationCode = "",
            newPassword = "123456",
            confirmPassword = "123456"
        )
        mainCoroutineRule.advanceUntilIdle()

        assertEquals("请输入验证码", viewModel.resetError.value)
    }

    @Test
    fun changePassword_shortNewPassword_setsValidationError() {
        viewModel.changePassword(
            token = "Bearer token",
            currentPassword = "oldpass1",
            newPassword = "12345",
            confirmPassword = "12345"
        )

        assertEquals("新密码长度至少 6 位", viewModel.errorMessage.value)
        verify(repository, never()).changePassword(any(), any(), any())
    }

    @Test
    fun changePassword_mismatchConfirm_setsValidationError() {
        viewModel.changePassword(
            token = "Bearer token",
            currentPassword = "oldpass1",
            newPassword = "newpass1",
            confirmPassword = "newpass2"
        )

        assertEquals("两次输入的新密码不一致", viewModel.errorMessage.value)
    }

    @Test
    fun changePassword_sameAsCurrent_setsValidationError() {
        viewModel.changePassword(
            token = "Bearer token",
            currentPassword = "samepass",
            newPassword = "samepass",
            confirmPassword = "samepass"
        )

        assertEquals("新密码不能与当前密码相同", viewModel.errorMessage.value)
    }

    @Test
    fun changePassword_success_setsAuthResult() {
        val call = mockCall<ApiAuthResponse>()
        whenever(repository.changePassword("token", "oldpass1", "newpass9")).thenReturn(call)
        enqueueSuccess(call, successAuth)

        viewModel.changePassword(
            token = "token",
            currentPassword = "oldpass1",
            newPassword = "newpass9",
            confirmPassword = "newpass9"
        )

        assertEquals(successAuth, viewModel.authResult.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun resetPassword_success_setsFlags() = runTest {
        whenever(repository.resetPassword("user@example.com", "123456", "newpass1"))
            .thenReturn(successAuth)

        viewModel.resetPassword(
            email = "user@example.com",
            verificationCode = "123456",
            newPassword = "newpass1",
            confirmPassword = "newpass1"
        )
        mainCoroutineRule.advanceUntilIdle()

        assertTrue(viewModel.resetSuccess.value!!)
        assertEquals(successAuth, viewModel.authResult.value)
        assertNull(viewModel.resetError.value)
    }

    @Test
    fun resetPassword_passwordMismatch_setsError() = runTest {
        viewModel.resetPassword(
            email = "user@example.com",
            verificationCode = "123456",
            newPassword = "newpass1",
            confirmPassword = "newpass2"
        )
        mainCoroutineRule.advanceUntilIdle()

        assertEquals("两次输入的密码不一致", viewModel.resetError.value)
        assertFalse(viewModel.resetSuccess.value!!)
    }

    @Test
    fun login_successWithNullBody_setsGenericError() {
        val call = mockCall<ApiAuthResponse>()
        whenever(repository.login(any(), any())).thenReturn(call)
        whenever(call.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<ApiAuthResponse>
            callback.onResponse(call, Response.success(null))
            null
        }

        viewModel.login("user@example.com", "123456")

        assertEquals("登录失败，服务返回空响应", viewModel.errorMessage.value)
        assertNull(viewModel.authResult.value)
    }

    @Test
    fun register_blankUsername_setsValidationError() {
        viewModel.register(
            username = "   ",
            nickname = "昵称",
            email = "user@example.com",
            password = "123456",
            confirm = "123456",
            agreedToTerms = true
        )

        assertEquals("请输入账号", viewModel.errorMessage.value)
        verify(repository, never()).register(any(), any(), any(), any(), any())
    }

    @Test
    fun register_blankNickname_setsValidationError() {
        viewModel.register(
            username = "newuser",
            nickname = "",
            email = "user@example.com",
            password = "123456",
            confirm = "123456",
            agreedToTerms = true
        )

        assertEquals("请输入昵称", viewModel.errorMessage.value)
    }

    @Test
    fun register_networkFailure_setsError() {
        val call = mockCall<ApiAuthResponse>()
        whenever(repository.register(any(), any(), any(), any(), any())).thenReturn(call)
        whenever(call.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<ApiAuthResponse>
            callback.onFailure(call, RuntimeException("connection reset"))
            null
        }

        viewModel.register(
            username = "newuser",
            nickname = "昵称",
            email = "user@example.com",
            password = "123456",
            confirm = "123456",
            agreedToTerms = true
        )

        assertEquals("connection reset", viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value!!)
    }

    @Test
    fun register_emptyBody_setsGenericError() {
        val call = mockCall<ApiAuthResponse>()
        whenever(repository.register(any(), any(), any(), any(), any())).thenReturn(call)
        whenever(call.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<ApiAuthResponse>
            callback.onResponse(call, Response.success(null))
            null
        }

        viewModel.register(
            username = "newuser",
            nickname = "昵称",
            email = "user@example.com",
            password = "123456",
            confirm = "123456",
            agreedToTerms = true
        )

        assertEquals("注册失败，服务返回空响应", viewModel.errorMessage.value)
    }

    @Test
    fun changePassword_blankCurrentPassword_setsValidationError() {
        viewModel.changePassword(
            token = "Bearer token",
            currentPassword = "",
            newPassword = "newpass1",
            confirmPassword = "newpass1"
        )

        assertEquals("请输入当前密码", viewModel.errorMessage.value)
        verify(repository, never()).changePassword(any(), any(), any())
    }

    @Test
    fun changePassword_unauthorized_parsesServerMessage() {
        val call = mockCall<ApiAuthResponse>()
        whenever(repository.changePassword(any(), any(), any())).thenReturn(call)
        val errorBody = """{"message":"当前密码错误"}"""
            .toResponseBody("application/json".toMediaType())
        enqueueError(call, 401, errorBody)

        viewModel.changePassword(
            token = "token",
            currentPassword = "wrong-old",
            newPassword = "newpass9",
            confirmPassword = "newpass9"
        )

        assertEquals("当前密码错误", viewModel.errorMessage.value)
        assertNull(viewModel.authResult.value)
    }

    @Test
    fun changePassword_networkFailure_setsError() {
        val call = mockCall<ApiAuthResponse>()
        whenever(repository.changePassword(any(), any(), any())).thenReturn(call)
        whenever(call.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<ApiAuthResponse>
            callback.onFailure(call, RuntimeException("socket closed"))
            null
        }

        viewModel.changePassword(
            token = "token",
            currentPassword = "oldpass1",
            newPassword = "newpass9",
            confirmPassword = "newpass9"
        )

        assertEquals("socket closed", viewModel.errorMessage.value)
    }

    @Test
    fun sendVerificationCode_repositoryThrows_setsError() = runTest {
        whenever(repository.sendVerificationCode("user@example.com"))
            .thenThrow(RuntimeException("该邮箱未注册"))

        viewModel.sendVerificationCode("user@example.com")
        mainCoroutineRule.advanceUntilIdle()

        assertEquals("该邮箱未注册", viewModel.sendCodeError.value)
        assertFalse(viewModel.codeSent.value!!)
    }

    @Test
    fun resetPassword_shortPassword_setsError() = runTest {
        viewModel.resetPassword(
            email = "user@example.com",
            verificationCode = "123456",
            newPassword = "12345",
            confirmPassword = "12345"
        )
        mainCoroutineRule.advanceUntilIdle()

        assertEquals("新密码长度不能少于 6 位", viewModel.resetError.value)
    }

    @Test
    fun resetPassword_repositoryThrows_setsError() = runTest {
        whenever(repository.resetPassword(any(), any(), any()))
            .thenThrow(RuntimeException("验证码错误或已过期"))

        viewModel.resetPassword(
            email = "user@example.com",
            verificationCode = "000000",
            newPassword = "newpass1",
            confirmPassword = "newpass1"
        )
        mainCoroutineRule.advanceUntilIdle()

        assertEquals("验证码错误或已过期", viewModel.resetError.value)
        assertFalse(viewModel.resetSuccess.value!!)
    }

    private inline fun <reified T> mockCall(): Call<T> = mock()

    private fun <T> enqueueSuccess(call: Call<T>, body: T) {
        whenever(call.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<T>
            callback.onResponse(call, Response.success(body))
            null
        }
    }

    private fun <T> enqueueError(call: Call<T>, code: Int, errorBody: okhttp3.ResponseBody) {
        whenever(call.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<T>
            callback.onResponse(call, Response.error(code, errorBody))
            null
        }
    }
}
