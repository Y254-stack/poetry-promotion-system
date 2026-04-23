package com.example.poetry.features.poem.repository

import com.example.poetry.core.network.ApiPoemDetailDto
import com.example.poetry.core.network.ApiTagDto
import com.example.poetry.core.network.ApiTagSearchResponse
import retrofit2.Call

interface PoemRepository {

    fun getHotTags(limit: Int = 20): Call<List<ApiTagDto>>

    fun searchByTags(
        tagIds: List<Long>,
        sort: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Call<ApiTagSearchResponse>

    fun getPoemDetail(workId: Long): Call<ApiPoemDetailDto>
}
