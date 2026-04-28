package com.example.poetry.features.user.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.poetry.core.network.ApiFavoritePoemDto
import com.example.poetry.core.network.ApiFavoritePostDto
import com.example.poetry.core.network.ApiFollowedUserDto
import com.example.poetry.core.network.ApiPagedResponse
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.features.user.mock.UserMockData
import com.example.poetry.features.user.model.FavoriteItemUiModel
import com.example.poetry.features.user.model.FavoriteTypeUi
import com.example.poetry.features.user.model.FollowUiModel
import com.example.poetry.features.user.model.ProgressStatUiModel
import com.example.poetry.features.user.model.UserCollectionUiModel
import com.example.poetry.features.user.model.UserQuickActionUiModel
import com.example.poetry.features.user.repository.UserCenterRepository
import com.example.poetry.features.user.repository.UserCenterRepositoryImpl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class UserViewModel : ViewModel() {

    private val _quickActions = MutableLiveData(UserMockData.quickActions())
    val quickActions: LiveData<List<UserQuickActionUiModel>> = _quickActions

    private val _homeProgressStats = MutableLiveData(UserMockData.homeProgressStats())
    val homeProgressStats: LiveData<List<ProgressStatUiModel>> = _homeProgressStats

    private val _dashboardExtraStats = MutableLiveData(UserMockData.dashboardExtraStats())
    val dashboardExtraStats: LiveData<List<ProgressStatUiModel>> = _dashboardExtraStats

    private val published = UserMockData.publishedCreations().toMutableList()
    private val drafts = UserMockData.draftCreations().toMutableList()

    private val _publishedCreations = MutableLiveData<List<UserCollectionUiModel>>(published.toList())
    val publishedCreations: LiveData<List<UserCollectionUiModel>> = _publishedCreations

    private val _draftCreations = MutableLiveData<List<UserCollectionUiModel>>(drafts.toList())
    val draftCreations: LiveData<List<UserCollectionUiModel>> = _draftCreations

    private val repository: UserCenterRepository = UserCenterRepositoryImpl(NetworkModule.poetryApiService)

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // region Favorites (posts/poems)
    enum class FavoriteTab { POSTS, POEMS }

    private val _favoriteTab = MutableLiveData(FavoriteTab.POSTS)
    val favoriteTab: LiveData<FavoriteTab> = _favoriteTab

    private val _favoriteQuery = MutableLiveData("")
    val favoriteQuery: LiveData<String> = _favoriteQuery

    private val _favoriteCount = MutableLiveData(0)
    val favoriteCount: LiveData<Int> = _favoriteCount

    private val _favoriteShownCount = MutableLiveData(0)
    val favoriteShownCount: LiveData<Int> = _favoriteShownCount

    private val _favoriteItems = MutableLiveData<List<FavoriteItemUiModel>>(emptyList())
    val favoriteItems: LiveData<List<FavoriteItemUiModel>> = _favoriteItems

    private val _favoriteHasMore = MutableLiveData(false)
    val favoriteHasMore: LiveData<Boolean> = _favoriteHasMore

    private val _favoriteIsLoadingMore = MutableLiveData(false)
    val favoriteIsLoadingMore: LiveData<Boolean> = _favoriteIsLoadingMore

    private var favoritePageSize = 20
    private var favoritePage = 1
    private var token: String? = null

    private var totalFavoriteCountFromServer: Int = 0
    private val favoritePostCache = mutableListOf<ApiFavoritePostDto>()
    private val favoritePoemCache = mutableListOf<ApiFavoritePoemDto>()
    // endregion

    private var followsPage = 1
    private val followsCache = mutableListOf<ApiFollowedUserDto>()
    private val _follows = MutableLiveData<List<FollowUiModel>>(emptyList())
    val follows: LiveData<List<FollowUiModel>> = _follows

    init {
        // 默认不请求，等待 setSessionToken()
    }

    fun setSessionToken(token: String?) {
        this.token = token?.trim().takeIf { !it.isNullOrBlank() }
        refreshFavorites(resetPaging = true)
        refreshFollows()
    }

    fun setFavoriteTab(tab: FavoriteTab) {
        if (_favoriteTab.value == tab) return
        _favoriteTab.value = tab
        _favoriteQuery.value = ""
        refreshFavorites(resetPaging = true)
    }

    fun setFavoriteQuery(query: String) {
        val q = query.trim()
        if (_favoriteQuery.value == q) return
        _favoriteQuery.value = q
        refreshFavorites(resetPaging = true)
    }

    fun loadMoreFavorites() {
        if (_favoriteIsLoadingMore.value == true) return
        if (_favoriteHasMore.value != true) return
        favoritePage += 1
        refreshFavorites(resetPaging = false)
    }

    fun unfavorite(item: FavoriteItemUiModel, onDone: (Boolean) -> Unit) {
        val t = token
        if (t.isNullOrBlank()) {
            _errorMessage.value = "请先登录"
            onDone(false)
            return
        }
        when (item.type) {
            FavoriteTypeUi.POEM -> {
                val wid = item.linkedPoemWorkId
                if (wid == null) {
                    onDone(false)
                    return
                }
                repository.unfavoritePoem(t, wid).enqueue(object : Callback<com.example.poetry.core.network.ApiOkResponse> {
                    override fun onResponse(
                        call: Call<com.example.poetry.core.network.ApiOkResponse>,
                        response: Response<com.example.poetry.core.network.ApiOkResponse>
                    ) {
                        val ok = response.isSuccessful && (response.body()?.ok == true)
                        if (ok) refreshFavorites(resetPaging = true) else _errorMessage.value = "取消收藏失败"
                        onDone(ok)
                    }

                    override fun onFailure(call: Call<com.example.poetry.core.network.ApiOkResponse>, t: Throwable) {
                        _errorMessage.value = t.message ?: "网络不可用"
                        onDone(false)
                    }
                })
            }

            FavoriteTypeUi.POST -> {
                val pid = item.linkedPostId
                if (pid == null) {
                    onDone(false)
                    return
                }
                repository.unfavoritePost(t, pid).enqueue(object : Callback<com.example.poetry.core.network.ApiOkResponse> {
                    override fun onResponse(
                        call: Call<com.example.poetry.core.network.ApiOkResponse>,
                        response: Response<com.example.poetry.core.network.ApiOkResponse>
                    ) {
                        val ok = response.isSuccessful && (response.body()?.ok == true)
                        if (ok) refreshFavorites(resetPaging = true) else _errorMessage.value = "取消收藏失败"
                        onDone(ok)
                    }

                    override fun onFailure(call: Call<com.example.poetry.core.network.ApiOkResponse>, t: Throwable) {
                        _errorMessage.value = t.message ?: "网络不可用"
                        onDone(false)
                    }
                })
            }
        }
    }

    fun refreshFavorites(resetPaging: Boolean) {
        val t = token
        if (t.isNullOrBlank()) {
            totalFavoriteCountFromServer = 0
            favoritePostCache.clear()
            favoritePoemCache.clear()
            _favoriteCount.value = 0
            _favoriteItems.value = emptyList()
            _favoriteHasMore.value = false
            return
        }

        if (resetPaging) {
            favoritePage = 1
            totalFavoriteCountFromServer = 0
            favoritePostCache.clear()
            favoritePoemCache.clear()
        }

        _favoriteIsLoadingMore.value = true
        val tab = _favoriteTab.value ?: FavoriteTab.POSTS
        when (tab) {
            FavoriteTab.POSTS -> {
                repository.getMyFavoritePosts(t, favoritePage, favoritePageSize)
                    .enqueue(object : Callback<ApiPagedResponse<ApiFavoritePostDto>> {
                        override fun onResponse(
                            call: Call<ApiPagedResponse<ApiFavoritePostDto>>,
                            response: Response<ApiPagedResponse<ApiFavoritePostDto>>
                        ) {
                            _favoriteIsLoadingMore.value = false
                            if (!response.isSuccessful) {
                                _errorMessage.value = "加载收藏失败：${response.code()}"
                                return
                            }
                            val body = response.body() ?: return
                            totalFavoriteCountFromServer = body.total
                            favoritePostCache.addAll(body.items)
                            publishFavoriteUi(tab)
                        }

                        override fun onFailure(call: Call<ApiPagedResponse<ApiFavoritePostDto>>, t: Throwable) {
                            _favoriteIsLoadingMore.value = false
                            _errorMessage.value = t.message ?: "网络不可用"
                        }
                    })
            }

            FavoriteTab.POEMS -> {
                repository.getMyFavoritePoems(t, favoritePage, favoritePageSize)
                    .enqueue(object : Callback<ApiPagedResponse<ApiFavoritePoemDto>> {
                        override fun onResponse(
                            call: Call<ApiPagedResponse<ApiFavoritePoemDto>>,
                            response: Response<ApiPagedResponse<ApiFavoritePoemDto>>
                        ) {
                            _favoriteIsLoadingMore.value = false
                            if (!response.isSuccessful) {
                                _errorMessage.value = "加载收藏失败：${response.code()}"
                                return
                            }
                            val body = response.body() ?: return
                            totalFavoriteCountFromServer = body.total
                            favoritePoemCache.addAll(body.items)
                            publishFavoriteUi(tab)
                        }

                        override fun onFailure(call: Call<ApiPagedResponse<ApiFavoritePoemDto>>, t: Throwable) {
                            _favoriteIsLoadingMore.value = false
                            _errorMessage.value = t.message ?: "网络不可用"
                        }
                    })
            }
        }
    }

    fun deletePublished(item: UserCollectionUiModel) {
        if (published.remove(item)) {
            _publishedCreations.value = published.toList()
        }
    }

    fun deleteDraft(item: UserCollectionUiModel) {
        if (drafts.remove(item)) {
            _draftCreations.value = drafts.toList()
        }
    }

    fun addDraftPlaceholder(title: String) {
        val item = UserCollectionUiModel(title, "刚刚创建", "草稿")
        drafts.add(0, item)
        _draftCreations.value = drafts.toList()
    }

    fun refreshFollows(pageSize: Int = 50) {
        val t = token
        if (t.isNullOrBlank()) {
            followsPage = 1
            followsCache.clear()
            _follows.value = emptyList()
            return
        }
        followsPage = 1
        followsCache.clear()
        repository.getMyFollows(t, followsPage, pageSize).enqueue(object : Callback<ApiPagedResponse<ApiFollowedUserDto>> {
            override fun onResponse(
                call: Call<ApiPagedResponse<ApiFollowedUserDto>>,
                response: Response<ApiPagedResponse<ApiFollowedUserDto>>
            ) {
                if (!response.isSuccessful) {
                    _errorMessage.value = "加载关注失败：${response.code()}"
                    return
                }
                val body = response.body() ?: return
                followsCache.addAll(body.items)
                _follows.value = followsCache.map { it.toFollowUi() }
            }

            override fun onFailure(call: Call<ApiPagedResponse<ApiFollowedUserDto>>, t: Throwable) {
                _errorMessage.value = t.message ?: "网络不可用"
            }
        })
    }

    fun unfollow(followedUserId: Long, onDone: (Boolean) -> Unit) {
        val t = token
        if (t.isNullOrBlank()) {
            _errorMessage.value = "请先登录"
            onDone(false)
            return
        }
        repository.unfollow(t, followedUserId).enqueue(object : Callback<com.example.poetry.core.network.ApiOkResponse> {
            override fun onResponse(
                call: Call<com.example.poetry.core.network.ApiOkResponse>,
                response: Response<com.example.poetry.core.network.ApiOkResponse>
            ) {
                val ok = response.isSuccessful && (response.body()?.ok == true)
                if (ok) refreshFollows() else _errorMessage.value = "取消关注失败"
                onDone(ok)
            }

            override fun onFailure(call: Call<com.example.poetry.core.network.ApiOkResponse>, t: Throwable) {
                _errorMessage.value = t.message ?: "网络不可用"
                onDone(false)
            }
        })
    }

    private fun publishFavoriteUi(tab: FavoriteTab) {
        val q = (_favoriteQuery.value ?: "").trim()
        val uiList = when (tab) {
            FavoriteTab.POSTS -> favoritePostCache.map { it.toFavoriteUi() }
            FavoriteTab.POEMS -> favoritePoemCache.map { it.toFavoriteUi() }
        }

        val filtered = if (q.isBlank()) uiList else uiList.filter {
            it.title.contains(q, ignoreCase = true) || it.subtitle.contains(q, ignoreCase = true)
        }

        val sorted = filtered.sortedWith(
            compareByDescending<FavoriteItemUiModel> { parseCollectedAtMillis(it.collectedAt) }
                .thenByDescending { it.id }
        )

        _favoriteCount.value = totalFavoriteCountFromServer
        _favoriteShownCount.value = sorted.size
        _favoriteItems.value = sorted
        _favoriteHasMore.value = when (tab) {
            FavoriteTab.POSTS -> favoritePostCache.size < totalFavoriteCountFromServer
            FavoriteTab.POEMS -> favoritePoemCache.size < totalFavoriteCountFromServer
        }
    }

    private fun ApiFollowedUserDto.toFollowUi(): FollowUiModel {
        val name = nickname.trim().ifBlank { username }
        val subtitle = if (username.isNotBlank()) "@$username" else email
        return FollowUiModel(
            userId = userId.toString(),
            displayName = name,
            subtitle = subtitle,
            roleBadge = "用户"
        )
    }

    private fun ApiFavoritePoemDto.toFavoriteUi(): FavoriteItemUiModel {
        val subtitle = "${authorName} · 收藏时间 ${formatCollectedAt(collectedAt)}"
        return FavoriteItemUiModel(
            id = workId,
            type = FavoriteTypeUi.POEM,
            title = title,
            subtitle = subtitle,
            collectedAt = collectedAt,
            linkedPoemWorkId = workId
        )
    }

    private fun ApiFavoritePostDto.toFavoriteUi(): FavoriteItemUiModel {
        val t = (title ?: "").trim().ifBlank { "帖子 #$postId" }
        val subtitle = "${authorNickname?.trim().orEmpty().ifBlank { "用户" }} · 收藏时间 ${formatCollectedAt(collectedAt)}"
        return FavoriteItemUiModel(
            id = postId,
            type = FavoriteTypeUi.POST,
            title = t,
            subtitle = subtitle,
            collectedAt = collectedAt,
            linkedPostId = postId
        )
    }

    private fun formatCollectedAt(raw: String?): String {
        val r = raw?.trim().orEmpty()
        if (r.isBlank()) return "未知"
        if ('%' in r) return "未知"

        val millis = parseCollectedAtMillis(r)
        if (millis <= 0L) return r

        val out = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return out.format(Date(millis))
    }

    private fun parseCollectedAtMillis(raw: String?): Long {
        val r = raw?.trim().orEmpty()
        if (r.isBlank()) return 0L
        if ('%' in r) return 0L

        // epoch seconds / millis
        r.toLongOrNull()?.let { n ->
            return when {
                n >= 1_000_000_000_000L -> n
                n >= 1_000_000_000L -> n * 1000L
                else -> 0L
            }
        }

        // Common server formats
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.getDefault())
                if (p.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val d = sdf.parse(r) ?: continue
                return d.time
            } catch (_: ParseException) {
                // try next
            } catch (_: IllegalArgumentException) {
                // try next
            }
        }
        return 0L
    }
}
