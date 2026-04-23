package com.example.poetry.features.poem.repository

import com.example.poetry.core.network.ApiPoemDetailDto
import com.example.poetry.core.network.ApiTagDto
import com.example.poetry.core.network.ApiTagSearchResponse
import com.example.poetry.core.network.PoetryApiService
import retrofit2.Call

class PoemRepositoryImpl(
    private val apiService: PoetryApiService
) : PoemRepository {

    override fun getHotTags(limit: Int): Call<List<ApiTagDto>> = apiService.getHotTags(limit)

    override fun searchByTags(
        tagIds: List<Long>,
        sort: String,
        page: Int,
        pageSize: Int
    ): Call<ApiTagSearchResponse> {
        val tagQuery = tagIds.joinToString(",")
        return apiService.searchPoemsByTags(tagQuery, sort, page, pageSize)
    }

    override fun getPoemDetail(workId: Long): Call<ApiPoemDetailDto> = apiService.getPoemDetail(workId)
}
