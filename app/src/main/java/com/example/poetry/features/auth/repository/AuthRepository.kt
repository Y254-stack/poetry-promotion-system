package com.example.poetry.features.auth.repository

import com.example.poetry.features.auth.model.AuthTipUiModel

interface AuthRepository {

    // TODO: 接入真实登录与注册能力
    fun getLoginTips(): List<AuthTipUiModel>

    // TODO: 接入真实找回密码流程
    fun requestPasswordReset(account: String)
}
