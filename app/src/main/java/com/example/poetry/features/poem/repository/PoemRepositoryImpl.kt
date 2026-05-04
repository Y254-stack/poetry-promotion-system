package com.example.poetry.features.poem.repository

import com.example.poetry.core.network.ApiPoemDetailDto
import com.example.poetry.core.network.PoetryApiService
import retrofit2.Call

class PoemRepositoryImpl(
    private val apiService: PoetryApiService
) : PoemRepository {

    override fun getHotTags(limit: Int): Call<List<ApiTagDto>> {
        return apiService.getHotTags(limit)
    }

    override fun searchByTags(
        tagIds: List<Long>,
        sort: String,
        page: Int,
        pageSize: Int
    ): Call<ApiTagSearchResponse> {
        val tagIdsStr = tagIds.joinToString(",")
        return apiService.searchByTags(tagIdsStr, sort, page, pageSize)
    }

    override fun searchByTitle(
        query: String,
        page: Int,
        pageSize: Int
    ): Call<ApiTitleSearchResponse> {
        return apiService.searchByTitle(query, page, pageSize)
    }

    override fun searchByAuthor(
        query: String,
        page: Int,
        pageSize: Int
    ): Call<ApiTitleSearchResponse> {
        return apiService.searchByAuthor(query, page, pageSize)
    }

    override fun searchByAll(
        query: String,
        page: Int,
        pageSize: Int
    ): Call<ApiTitleSearchResponse> {
        return apiService.searchByAll(query, page, pageSize)
    }

    override fun getPoemDetail(workId: Long): Call<ApiPoemDetailDto> {
        return apiService.getPoemDetail(workId)
    }
}
