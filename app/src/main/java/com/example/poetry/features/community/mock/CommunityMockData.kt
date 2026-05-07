package com.example.poetry.features.community.mock

import com.example.poetry.features.community.model.CommentUiModel
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.community.model.NotificationUiModel
import java.util.Date

object CommunityMockData {

    fun posts(): List<CommunityPostUiModel> = listOf(
        CommunityPostUiModel(
            postId = 1L,
            userId = 1L,
            author = "清风诗社",
            title = "你最喜欢的春日诗句是哪一句？",
            preview = "适合内容广场卡片流布局，后续接评论、点赞与收藏。",
            tag = "话题",
            viewCount = 128,
            likeCount = 56,
            commentCount = 23,
            collectCount = 12,
            createdAt = Date()
        ),
        CommunityPostUiModel(
            postId = 2L,
            userId = 2L,
            author = "古文创作坊",
            title = "分享一下你的诗词卡片排版灵感",
            preview = "用于发帖页和详情页的联动演示。",
            tag = "创作",
            viewCount = 89,
            likeCount = 34,
            commentCount = 8,
            collectCount = 5,
            createdAt = Date(System.currentTimeMillis() - 3600000)
        ),
        CommunityPostUiModel(
            postId = 3L,
            userId = 3L,
            author = "学习打卡营",
            title = "本周背诵目标挑战",
            preview = "后续可与学习进度和打卡系统结合。",
            tag = "活动",
            viewCount = 245,
            likeCount = 102,
            commentCount = 45,
            collectCount = 28,
            createdAt = Date(System.currentTimeMillis() - 86400000)
        )
    )

    fun notifications(): List<NotificationUiModel> = listOf(
        NotificationUiModel(
            id = 1L,
            title = "新的回复",
            summary = "你的帖子收到了一条新评论。",
            time = "今天 09:20"
        ),
        NotificationUiModel(
            id = 2L,
            title = "系统推荐",
            summary = "今日推荐诗词已经为你生成。",
            time = "今天 08:00"
        ),
        NotificationUiModel(
            id = 3L,
            title = "活动提醒",
            summary = "飞花令挑战赛今晚八点开始。",
            time = "昨天 19:30"
        )
    )
}