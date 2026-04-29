package com.example.poetry.features.poem.repository

import com.example.poetry.core.network.ApiPoemDetailDto
import com.example.poetry.core.network.PoetryApiService
import retrofit2.Call

class PoemRepositoryImpl(
    private val apiService: PoetryApiService
) : PoemRepository {

    override fun getPoemDetail(workId: Long): Call<ApiPoemDetailDto> {
        return apiService.getPoemDetail(workId)
    }
}
