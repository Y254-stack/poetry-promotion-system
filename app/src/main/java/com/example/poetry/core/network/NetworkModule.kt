package com.example.poetry.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // 尝试使用 10.0.2.2。如果仍然失败，请在 Logcat 确认错误信息
    private const val BASE_URL = "http://10.0.2.2:8081/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // 设置为 BODY 级别以在 Logcat 查看完整的请求和返回内容
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 针对冷启动场景优化超时时间
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        // 连接超时：首次连接可能需要等待数据库初始化
        .connectTimeout(30, TimeUnit.SECONDS)
        // 读取超时：考虑到数据库查询可能较慢
        .readTimeout(30, TimeUnit.SECONDS)
        // 写入超时
        .writeTimeout(30, TimeUnit.SECONDS)
        // 添加日志拦截器
        .addInterceptor(loggingInterceptor)
        // 允许重试（针对瞬时网络问题）
        .retryOnConnectionFailure(true)
        // 连接池大小
        .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val poetryApiService: PoetryApiService = retrofit.create(PoetryApiService::class.java)
}