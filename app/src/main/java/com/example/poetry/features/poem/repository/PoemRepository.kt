package com.example.poetry.features.poem.repository

import com.example.poetry.features.poem.model.AuthorProfileUiModel
import com.example.poetry.features.poem.model.PoemDetailUiModel
import com.example.poetry.features.poem.model.PoemSummaryUiModel

interface PoemRepository {

    // TODO: 接入真实诗词详情
    fun getPoemDetail(): PoemDetailUiModel

    // TODO: 接入真实搜索
    fun search(keyword: String): List<PoemSummaryUiModel>

    // TODO: 接入真实作者信息
    fun getAuthorProfile(): AuthorProfileUiModel
}
