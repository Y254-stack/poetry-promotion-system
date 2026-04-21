package com.example.poetry.features.auth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.features.auth.mock.AuthMockData
import com.example.poetry.features.auth.model.AuthTipUiModel

class AuthViewModel : ViewModel() {

    private val _tips = MutableLiveData(AuthMockData.loginTips())
    val tips: LiveData<List<AuthTipUiModel>> = _tips
}
