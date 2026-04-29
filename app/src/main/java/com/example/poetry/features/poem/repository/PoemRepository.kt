package com.example.poetry.features.poem.repository

import com.example.poetry.core.network.ApiPoemDetailDto
import retrofit2.Call

interface PoemRepository {

    fun getPoemDetail(workId: Long): Call<ApiPoemDetailDto>
}
