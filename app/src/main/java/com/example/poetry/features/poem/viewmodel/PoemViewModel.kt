package com.example.poetry.features.poem.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.core.network.ApiPoemDetailDto
import com.example.poetry.core.network.ApiPoemSearchItemDto
import com.example.poetry.core.network.ApiTagDto
import com.example.poetry.core.network.ApiTagSearchResponse
import com.example.poetry.core.network.ApiTitleSearchResponse
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.features.poem.mock.PoemMockData
import com.example.poetry.features.poem.model.AuthorProfileUiModel
import com.example.poetry.features.poem.model.PoemDetailUiModel
import com.example.poetry.features.poem.model.PoemSummaryUiModel
import com.example.poetry.features.poem.model.TagSearchSort
import com.example.poetry.features.poem.model.TagSearchUiState
import com.example.poetry.features.poem.model.TagUiModel
import com.example.poetry.features.poem.model.TitleSearchUiState
import com.example.poetry.features.poem.repository.PoemRepository
import com.example.poetry.features.poem.repository.PoemRepositoryImpl
import com.example.poetry.features.favorite.repository.FavoriteRepository
import com.example.poetry.features.favorite.repository.FavoriteRepositoryImpl
import com.example.poetry.core.network.ApiFavoriteCheckResponse
import com.example.poetry.core.network.ApiFavoriteActionResponse
import kotlin.math.ceil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PoemViewModel(
    private val repository: PoemRepository = PoemRepositoryImpl(NetworkModule.poetryApiService),
    private val favoriteRepository: FavoriteRepository = FavoriteRepositoryImpl(NetworkModule.poetryApiService)
) : ViewModel() {

    companion object {
        private const val TAG = "PoemViewModel"
    }

    private val _poemDetail = MutableLiveData(PoemMockData.emptyPoemDetail())
    val poemDetail: LiveData<PoemDetailUiModel> = _poemDetail

    private val _isFavorited = MutableLiveData(false)
    val isFavorited: LiveData<Boolean> = _isFavorited

    private val _searchUiState = MutableLiveData(
        TagSearchUiState(
            availableTags = emptyList(),
            results = emptyList(),
            recommendedTags = emptyList()
        )
    )
    val searchUiState: LiveData<TagSearchUiState> = _searchUiState

    private val _titleSearchUiState = MutableLiveData(TitleSearchUiState())
    val titleSearchUiState: LiveData<TitleSearchUiState> = _titleSearchUiState

    private val _authorProfile = MutableLiveData(PoemMockData.emptyAuthorProfile())
    val authorProfile: LiveData<AuthorProfileUiModel> = _authorProfile

    private val _representativeWorks = MutableLiveData<List<PoemSummaryUiModel>>(emptyList())
    val representativeWorks: LiveData<List<PoemSummaryUiModel>> = _representativeWorks

    fun loadHotTags() {
        val current = _searchUiState.value ?: TagSearchUiState()
        _searchUiState.value = current.copy(isLoading = true, errorMessage = null)
        repository.getHotTags().enqueue(object : Callback<List<ApiTagDto>> {
            override fun onResponse(call: Call<List<ApiTagDto>>, response: Response<List<ApiTagDto>>) {
                Log.d(TAG, "loadHotTags success: code=${response.code()}, size=${response.body()?.size ?: 0}")
                val tags = response.body().orEmpty().map { it.toUiModel() }
                val latest = _searchUiState.value ?: TagSearchUiState()
                _searchUiState.value = latest.copy(
                    availableTags = if (tags.isEmpty()) latest.availableTags else tags,
                    recommendedTags = if (tags.isEmpty()) latest.recommendedTags else tags.take(8),
                    isLoading = false,
                    errorMessage = null
                )
            }

            override fun onFailure(call: Call<List<ApiTagDto>>, t: Throwable) {
                Log.e(TAG, "loadHotTags failed", t)
                val latest = _searchUiState.value ?: TagSearchUiState()
                _searchUiState.value = latest.copy(
                    availableTags = latest.availableTags,
                    recommendedTags = emptyList(),
                    isLoading = false,
                    errorMessage = "标签服务暂不可用，请稍后重试。"
                )
            }
        })
    }

    fun toggleTagSelectionOnly(tagId: Long) {
        val current = _searchUiState.value ?: return
        val nextSelected = current.selectedTagIds.toMutableList().apply {
            if (contains(tagId)) remove(tagId) else add(tagId)
        }
        _searchUiState.value = current.copy(
            selectedTagIds = nextSelected,
            errorMessage = null
        )
    }

    fun updateSort(sort: TagSearchSort) {
        val current = _searchUiState.value ?: return
        _searchUiState.value = current.copy(sort = sort)
        if (current.selectedTagIds.isNotEmpty()) {
            searchByTags(current.selectedTagIds, sort, page = 1)
        }
    }

    fun loadPoemDetail(workId: Long) {
        repository.getPoemDetail(workId).enqueue(object : Callback<ApiPoemDetailDto> {
            override fun onResponse(call: Call<ApiPoemDetailDto>, response: Response<ApiPoemDetailDto>) {
                Log.d(TAG, "loadPoemDetail success: code=${response.code()}, workId=$workId")
                response.body()?.let { _poemDetail.value = it.toUiModel() }
            }

            override fun onFailure(call: Call<ApiPoemDetailDto>, t: Throwable) {
                Log.e(TAG, "loadPoemDetail failed: workId=$workId", t)
                _poemDetail.value = PoemMockData.emptyPoemDetail()
            }
        })
    }

    fun searchByTags(
        tagIds: List<Long>,
        sort: TagSearchSort,
        page: Int = 1
    ) {
        val current = _searchUiState.value ?: TagSearchUiState()
        _searchUiState.value = current.copy(
            selectedTagIds = tagIds,
            sort = sort,
            currentPage = page,
            isLoading = true,
            emptyMessage = null,
            errorMessage = null,
            results = emptyList(),
            recommendedTags = emptyList(),
            totalCount = 0,
            totalPages = 0
        )

        repository.searchByTags(
            tagIds = tagIds,
            sort = sort.apiValue,
            page = page,
            pageSize = current.pageSize
        ).enqueue(object : Callback<ApiTagSearchResponse> {
            override fun onResponse(call: Call<ApiTagSearchResponse>, response: Response<ApiTagSearchResponse>) {
                Log.d(TAG, "searchByTags success: code=${response.code()}, tagIds=$tagIds, sort=${sort.apiValue}, page=$page")
                val body = response.body()
                if (body == null) {
                    Log.e(TAG, "searchByTags empty body: tagIds=$tagIds, sort=${sort.apiValue}, page=$page")
                    applySearchError(current, tagIds, sort, page)
                    return
                }

                val totalPages = if (body.total == 0) 0 else ceil(body.total.toDouble() / body.pageSize).toInt()
                _searchUiState.value = current.copy(
                    selectedTagIds = tagIds,
                    sort = sort,
                    currentPage = body.page,
                    pageSize = body.pageSize,
                    totalCount = body.total,
                    totalPages = totalPages,
                    results = body.items.map { it.toUiModel() },
                    recommendedTags = body.recommendedTags.map { it.toUiModel() },
                    emptyMessage = body.emptyMessage,
                    isLoading = false,
                    errorMessage = null
                )
            }

            override fun onFailure(call: Call<ApiTagSearchResponse>, t: Throwable) {
                Log.e(TAG, "searchByTags failed: tagIds=$tagIds, sort=${sort.apiValue}, page=$page", t)
                applySearchError(current, tagIds, sort, page)
            }
        })
    }

    private fun applySearchError(
        current: TagSearchUiState,
        tagIds: List<Long>,
        sort: TagSearchSort,
        page: Int
    ) {
        _searchUiState.value = current.copy(
            selectedTagIds = tagIds,
            sort = sort,
            currentPage = page,
            results = emptyList(),
            recommendedTags = emptyList(),
            totalCount = 0,
            totalPages = 0,
            emptyMessage = "未找到同时匹配所选标签的诗词",
            isLoading = false,
            errorMessage = "搜索服务暂不可用，请稍后重试。"
        )
    }

    private fun ApiTagDto.toUiModel(): TagUiModel =
        TagUiModel(tagId = tagId, tagName = tagName, tagType = tagType, workCount = workCount)

    private fun ApiPoemSearchItemDto.toUiModel(): PoemSummaryUiModel =
        PoemSummaryUiModel(
            workId = workId,
            title = title,
            author = authorName,
            dynasty = dynastyName,
            snippet = contentPreview,
            matchedTags = matchedTags ?: "",
            hotScore = hotScore,
            publishTime = publishTime ?: ""
        )

    private fun ApiPoemDetailDto.toUiModel(): PoemDetailUiModel =
        PoemDetailUiModel(
            workId = workId,
            title = title,
            author = authorName,
            dynasty = dynastyName,
            content = contentText,
            translation = translationText.orEmpty(),
            annotation = annotationText.orEmpty(),
            appreciation = appreciationText.orEmpty()
        )

    fun searchByTitle(query: String, page: Int = 1) {
        val current = _titleSearchUiState.value ?: TitleSearchUiState()
        _titleSearchUiState.value = current.copy(
            query = query,
            currentPage = page,
            isLoading = true,
            errorMessage = null,
            emptyMessage = null,
            results = emptyList()
        )

        repository.searchByTitle(
            query = query,
            page = page,
            pageSize = current.pageSize
        ).enqueue(object : Callback<ApiTitleSearchResponse> {
            override fun onResponse(call: Call<ApiTitleSearchResponse>, response: Response<ApiTitleSearchResponse>) {
                Log.d(TAG, "searchByTitle success: code=${response.code()}, query=$query, page=$page")
                val body = response.body()
                if (body == null) {
                    Log.e(TAG, "searchByTitle empty body: query=$query, page=$page")
                    applyTitleSearchError(current, query, page)
                    return
                }

                val totalPages = if (body.total == 0) 0 else ceil(body.total.toDouble() / body.pageSize).toInt()
                _titleSearchUiState.value = current.copy(
                    query = query,
                    currentPage = body.page,
                    pageSize = body.pageSize,
                    totalCount = body.total,
                    totalPages = totalPages,
                    results = body.items.map { it.toUiModel() },
                    emptyMessage = body.emptyMessage,
                    isLoading = false,
                    errorMessage = null
                )
            }

            override fun onFailure(call: Call<ApiTitleSearchResponse>, t: Throwable) {
                Log.e(TAG, "searchByTitle failed: query=$query, page=$page", t)
                applyTitleSearchError(current, query, page)
            }
        })
    }

    private fun applyTitleSearchError(
        current: TitleSearchUiState,
        query: String,
        page: Int
    ) {
        _titleSearchUiState.value = current.copy(
            query = query,
            currentPage = page,
            results = emptyList(),
            totalCount = 0,
            totalPages = 0,
            emptyMessage = "未找到包含「$query」的诗词",
            isLoading = false,
            errorMessage = "搜索服务暂不可用，请稍后重试。"
        )
    }

    fun searchByAuthor(query: String, page: Int = 1) {
        val current = _titleSearchUiState.value ?: TitleSearchUiState()
        _titleSearchUiState.value = current.copy(
            query = query,
            currentPage = page,
            isLoading = true,
            errorMessage = null,
            emptyMessage = null,
            results = emptyList()
        )

        repository.searchByAuthor(
            query = query,
            page = page,
            pageSize = current.pageSize
        ).enqueue(object : Callback<ApiTitleSearchResponse> {
            override fun onResponse(call: Call<ApiTitleSearchResponse>, response: Response<ApiTitleSearchResponse>) {
                Log.d(TAG, "searchByAuthor success: code=${response.code()}, query=$query, page=$page")
                val body = response.body()
                if (body == null) {
                    Log.e(TAG, "searchByAuthor empty body: query=$query, page=$page")
                    applyAuthorSearchError(current, query, page)
                    return
                }

                val totalPages = if (body.total == 0) 0 else ceil(body.total.toDouble() / body.pageSize).toInt()
                _titleSearchUiState.value = current.copy(
                    query = query,
                    currentPage = body.page,
                    pageSize = body.pageSize,
                    totalCount = body.total,
                    totalPages = totalPages,
                    results = body.items.map { it.toUiModel() },
                    emptyMessage = body.emptyMessage,
                    isLoading = false,
                    errorMessage = null
                )
            }

            override fun onFailure(call: Call<ApiTitleSearchResponse>, t: Throwable) {
                Log.e(TAG, "searchByAuthor failed: query=$query, page=$page", t)
                applyAuthorSearchError(current, query, page)
            }
        })
    }

    private fun applyAuthorSearchError(
        current: TitleSearchUiState,
        query: String,
        page: Int
    ) {
        _titleSearchUiState.value = current.copy(
            query = query,
            currentPage = page,
            results = emptyList(),
            totalCount = 0,
            totalPages = 0,
            emptyMessage = "未找到作者「$query」的诗词",
            isLoading = false,
            errorMessage = "搜索服务暂不可用，请稍后重试。"
        )
    }

    fun searchByAll(query: String, page: Int = 1) {
        val current = _titleSearchUiState.value ?: TitleSearchUiState()
        _titleSearchUiState.value = current.copy(
            query = query,
            currentPage = page,
            isLoading = true,
            errorMessage = null,
            emptyMessage = null,
            results = emptyList()
        )

        repository.searchByAll(
            query = query,
            page = page,
            pageSize = current.pageSize
        ).enqueue(object : Callback<ApiTitleSearchResponse> {
            override fun onResponse(call: Call<ApiTitleSearchResponse>, response: Response<ApiTitleSearchResponse>) {
                Log.d(TAG, "searchByAll success: code=${response.code()}, query=$query, page=$page")
                val body = response.body()
                if (body == null) {
                    Log.e(TAG, "searchByAll empty body: query=$query, page=$page")
                    applyAllSearchError(current, query, page)
                    return
                }

                val totalPages = if (body.total == 0) 0 else ceil(body.total.toDouble() / body.pageSize).toInt()
                _titleSearchUiState.value = current.copy(
                    query = query,
                    currentPage = body.page,
                    pageSize = body.pageSize,
                    totalCount = body.total,
                    totalPages = totalPages,
                    results = body.items.map { it.toUiModel() },
                    emptyMessage = body.emptyMessage,
                    isLoading = false,
                    errorMessage = null
                )
            }

            override fun onFailure(call: Call<ApiTitleSearchResponse>, t: Throwable) {
                Log.e(TAG, "searchByAll failed: query=$query, page=$page", t)
                applyAllSearchError(current, query, page)
            }
        })
    }

    private fun applyAllSearchError(
        current: TitleSearchUiState,
        query: String,
        page: Int
    ) {
        _titleSearchUiState.value = current.copy(
            query = query,
            currentPage = page,
            results = emptyList(),
            totalCount = 0,
            totalPages = 0,
            emptyMessage = "未找到包含「$query」的诗词或作者",
            isLoading = false,
            errorMessage = "搜索服务暂不可用，请稍后重试。"
        )
    }

    fun searchByDynasty(dynastyName: String, page: Int = 1) {
        val current = _titleSearchUiState.value ?: TitleSearchUiState()
        _titleSearchUiState.value = current.copy(
            query = dynastyName,
            currentPage = page,
            isLoading = true,
            errorMessage = null,
            emptyMessage = null,
            results = emptyList()
        )

        NetworkModule.poetryApiService.getPoemsByDynasty(
            dynastyName = dynastyName,
            page = page,
            pageSize = current.pageSize
        ).enqueue(object : Callback<ApiTitleSearchResponse> {
            override fun onResponse(call: Call<ApiTitleSearchResponse>, response: Response<ApiTitleSearchResponse>) {
                Log.d(TAG, "searchByDynasty success: code=${response.code()}, dynastyName=$dynastyName, page=$page")
                val body = response.body()
                if (body == null) {
                    Log.e(TAG, "searchByDynasty empty body: dynastyName=$dynastyName, page=$page")
                    applyDynastySearchError(current, dynastyName, page)
                    return
                }

                val totalPages = if (body.total == 0) 0 else ceil(body.total.toDouble() / body.pageSize).toInt()
                _titleSearchUiState.value = current.copy(
                    query = dynastyName,
                    currentPage = body.page,
                    pageSize = body.pageSize,
                    totalCount = body.total,
                    totalPages = totalPages,
                    results = body.items.map { it.toUiModel() },
                    emptyMessage = if (body.total == 0) "未找到「$dynastyName」朝代的诗词" else null,
                    isLoading = false,
                    errorMessage = null
                )
            }

            override fun onFailure(call: Call<ApiTitleSearchResponse>, t: Throwable) {
                Log.e(TAG, "searchByDynasty failed: dynastyName=$dynastyName, page=$page", t)
                applyDynastySearchError(current, dynastyName, page)
            }
        })
    }

    private fun applyDynastySearchError(
        current: TitleSearchUiState,
        dynastyName: String,
        page: Int
    ) {
        _titleSearchUiState.value = current.copy(
            query = dynastyName,
            currentPage = page,
            results = emptyList(),
            totalCount = 0,
            totalPages = 0,
            emptyMessage = "未找到「$dynastyName」朝代的诗词",
            isLoading = false,
            errorMessage = "搜索服务暂不可用，请稍后重试。"
        )
    }

    fun searchByAuthorId(authorId: Long, authorName: String, page: Int = 1) {
        val current = _titleSearchUiState.value ?: TitleSearchUiState()
        _titleSearchUiState.value = current.copy(
            query = authorName,
            currentPage = page,
            isLoading = true,
            errorMessage = null,
            emptyMessage = null,
            results = emptyList()
        )

        NetworkModule.poetryApiService.getPoemsByAuthor(
            authorId = authorId,
            page = page,
            pageSize = current.pageSize
        ).enqueue(object : Callback<ApiTitleSearchResponse> {
            override fun onResponse(call: Call<ApiTitleSearchResponse>, response: Response<ApiTitleSearchResponse>) {
                Log.d(TAG, "searchByAuthorId success: code=${response.code()}, authorId=$authorId, page=$page")
                val body = response.body()
                if (body == null) {
                    Log.e(TAG, "searchByAuthorId empty body: authorId=$authorId, page=$page")
                    applyAuthorIdSearchError(current, authorName, page)
                    return
                }

                val totalPages = if (body.total == 0) 0 else ceil(body.total.toDouble() / body.pageSize).toInt()
                _titleSearchUiState.value = current.copy(
                    query = authorName,
                    currentPage = body.page,
                    pageSize = body.pageSize,
                    totalCount = body.total,
                    totalPages = totalPages,
                    results = body.items.map { it.toUiModel() },
                    emptyMessage = if (body.total == 0) "未找到「$authorName」的作品" else null,
                    isLoading = false,
                    errorMessage = null
                )
            }

            override fun onFailure(call: Call<ApiTitleSearchResponse>, t: Throwable) {
                Log.e(TAG, "searchByAuthorId failed: authorId=$authorId, page=$page", t)
                applyAuthorIdSearchError(current, authorName, page)
            }
        })
    }

    private fun applyAuthorIdSearchError(
        current: TitleSearchUiState,
        authorName: String,
        page: Int
    ) {
        _titleSearchUiState.value = current.copy(
            query = authorName,
            currentPage = page,
            results = emptyList(),
            totalCount = 0,
            totalPages = 0,
            emptyMessage = "未找到「$authorName」的作品",
            isLoading = false,
            errorMessage = "搜索服务暂不可用，请稍后重试。"
        )
    }

    fun checkFavoriteStatus(authHeader: String?, workId: Long) {
        if (authHeader.isNullOrBlank()) {
            _isFavorited.value = false
            return
        }
        favoriteRepository.checkMyFavorite(authHeader, workId).enqueue(object : Callback<ApiFavoriteCheckResponse> {
            override fun onResponse(call: Call<ApiFavoriteCheckResponse>, response: Response<ApiFavoriteCheckResponse>) {
                Log.d(TAG, "checkMyFavorite success: workId=$workId, code=${response.code()}")
                if (response.isSuccessful) {
                    _isFavorited.value = response.body()?.isFavorited ?: false
                } else {
                    _isFavorited.value = false
                }
            }

            override fun onFailure(call: Call<ApiFavoriteCheckResponse>, t: Throwable) {
                Log.e(TAG, "checkMyFavorite failed: workId=$workId", t)
                _isFavorited.value = false
            }
        })
    }

    fun toggleFavorite(authHeader: String?, workId: Long) {
        if (authHeader.isNullOrBlank()) {
            return
        }
        val currentStatus = _isFavorited.value ?: false
        if (currentStatus) {
            favoriteRepository.removeMyFavorite(authHeader, workId).enqueue(object : Callback<ApiFavoriteActionResponse> {
                override fun onResponse(call: Call<ApiFavoriteActionResponse>, response: Response<ApiFavoriteActionResponse>) {
                    Log.d(TAG, "removeMyFavorite success: workId=$workId, code=${response.code()}")
                    if (response.isSuccessful && response.body()?.success == true) {
                        _isFavorited.value = false
                    }
                }

                override fun onFailure(call: Call<ApiFavoriteActionResponse>, t: Throwable) {
                    Log.e(TAG, "removeMyFavorite failed: workId=$workId", t)
                }
            })
        } else {
            favoriteRepository.addMyFavorite(authHeader, workId).enqueue(object : Callback<ApiFavoriteActionResponse> {
                override fun onResponse(call: Call<ApiFavoriteActionResponse>, response: Response<ApiFavoriteActionResponse>) {
                    Log.d(TAG, "addMyFavorite success: workId=$workId, code=${response.code()}")
                    if (response.isSuccessful && response.body()?.success == true) {
                        _isFavorited.value = true
                    }
                }

                override fun onFailure(call: Call<ApiFavoriteActionResponse>, t: Throwable) {
                    Log.e(TAG, "addMyFavorite failed: workId=$workId", t)
                }
            })
        }
    }
}
