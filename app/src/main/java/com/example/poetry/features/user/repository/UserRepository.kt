package com.example.poetry.features.user.repository

import com.example.poetry.features.user.model.ProgressStatUiModel
import com.example.poetry.features.user.model.UserCollectionUiModel
import com.example.poetry.features.user.model.UserQuickActionUiModel

interface UserRepository {

    // TODO: 接入真实用户中心数据
    fun getQuickActions(): List<UserQuickActionUiModel>

    // TODO: 接入真实学习统计
    fun getProgressStats(): List<ProgressStatUiModel>

    // TODO: 接入真实收藏、创作和关注数据
    fun getCollections(type: String): List<UserCollectionUiModel>
}
