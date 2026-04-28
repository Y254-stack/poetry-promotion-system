package com.example.poetry.features.user.local

import java.util.concurrent.ConcurrentHashMap

/**
 * 仅用于“后端未接入前”的本地演示数据源。
 * - 视作“本地数据库”：写入后，列表/资料读取会同步变化
 * - 当前为进程内内存数据；后续接入 Room/后端接口时可替换实现
 */
object UserLocalStore {

    data class Account(
        val userId: Long,
        val username: String,
        val email: String? = null,
        var nickname: String
    )

    enum class FavoriteType { POST, POEM }

    data class FavoriteItem(
        val id: Long,
        val type: FavoriteType,
        val title: String,
        val subtitle: String,
        val collectedAtMillis: Long,
        val keywords: String = "",
        val linkedPoemWorkId: Long? = null,
        val linkedPostId: Long? = null
    )

    private val accounts = ConcurrentHashMap<Long, Account>()
    private val favoritesByUser = ConcurrentHashMap<Long, MutableList<FavoriteItem>>()

    fun ensureSeed(userId: Long, username: String, nickname: String) {
        if (!accounts.containsKey(userId)) {
            accounts[userId] = Account(userId = userId, username = username, nickname = nickname)
        }
        if (!favoritesByUser.containsKey(userId)) {
            favoritesByUser[userId] = seedFavorites()
        }
    }

    fun getAccount(userId: Long): Account? = accounts[userId]

    fun updateNickname(userId: Long, nickname: String): Boolean {
        val acc = accounts[userId] ?: return false
        acc.nickname = nickname
        return true
    }

    fun deleteAccount(userId: Long) {
        accounts.remove(userId)
        favoritesByUser.remove(userId)
    }

    fun listFavorites(userId: Long, type: FavoriteType): List<FavoriteItem> {
        val list = favoritesByUser[userId].orEmpty()
        return list.filter { it.type == type }.sortedByDescending { it.collectedAtMillis }
    }

    fun removeFavorite(userId: Long, favoriteId: Long): Boolean {
        val list = favoritesByUser[userId] ?: return false
        return list.removeIf { it.id == favoriteId }
    }

    private fun seedFavorites(): MutableList<FavoriteItem> {
        val now = System.currentTimeMillis()
        // 时间越大越靠前（最近收藏在上）
        return mutableListOf(
            FavoriteItem(
                id = 1001L,
                type = FavoriteType.POST,
                title = "你最喜欢的春日诗句是哪一句？",
                subtitle = "清风诗社 · 收藏时间 04-18",
                collectedAtMillis = now - 3L * 24 * 60 * 60 * 1000,
                keywords = "清风诗社 春日 诗句 话题",
                linkedPostId = 1L
            ),
            FavoriteItem(
                id = 1002L,
                type = FavoriteType.POST,
                title = "分享一下你的诗词卡片排版灵感",
                subtitle = "古文创作坊 · 收藏时间 04-12",
                collectedAtMillis = now - 9L * 24 * 60 * 60 * 1000,
                keywords = "古文创作坊 卡片 排版 创作",
                linkedPostId = 2L
            ),
            FavoriteItem(
                id = 2001L,
                type = FavoriteType.POEM,
                title = "望岳",
                subtitle = "杜甫 · 收藏时间 04-18",
                collectedAtMillis = now - 3L * 24 * 60 * 60 * 1000,
                keywords = "望岳 杜甫 岱宗",
                linkedPoemWorkId = 1L
            ),
            FavoriteItem(
                id = 2002L,
                type = FavoriteType.POEM,
                title = "春夜喜雨",
                subtitle = "杜甫 · 收藏时间 04-11",
                collectedAtMillis = now - 10L * 24 * 60 * 60 * 1000,
                keywords = "春夜喜雨 杜甫 好雨知时节",
                linkedPoemWorkId = 2L
            )
        )
    }
}

