package com.example.poetry.features.community.mock

import com.example.poetry.features.community.model.CommentUiModel
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.community.model.NotificationUiModel

object CommunityMockData {

    fun posts(): List<CommunityPostUiModel> = listOf(
        CommunityPostUiModel("清风诗社", "你最喜欢的春日诗句是哪一句？", "适合内容广场卡片流布局，后续接评论、点赞与收藏。", "话题"),
        CommunityPostUiModel("古文创作坊", "分享一下你的诗词卡片排版灵感", "用于发帖页和详情页的联动演示。", "创作"),
        CommunityPostUiModel("学习打卡营", "本周背诵目标挑战", "后续可与学习进度和打卡系统结合。", "活动")
    )

    fun notifications(): List<NotificationUiModel> = listOf(
        NotificationUiModel("新的回复", "你的帖子收到了一条新评论。", "今天 09:20"),
        NotificationUiModel("系统推荐", "今日推荐诗词已经为你生成。", "今天 08:00"),
        NotificationUiModel("活动提醒", "飞花令挑战赛今晚八点开始。", "昨天 19:30")
    )

    fun comments(): List<CommentUiModel> = listOf(
        CommentUiModel("诗友甲", "我最喜欢“竹外桃花三两枝”。", "2 分钟前"),
        CommentUiModel("诗友乙", "这张卡片的配色很适合春天。", "10 分钟前")
    )
}
