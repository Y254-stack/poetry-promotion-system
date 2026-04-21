package com.example.poetry.features.poem.mock

import com.example.poetry.features.poem.model.AuthorProfileUiModel
import com.example.poetry.features.poem.model.PoemDetailUiModel
import com.example.poetry.features.poem.model.PoemSummaryUiModel

object PoemMockData {

    fun poemDetail(): PoemDetailUiModel = PoemDetailUiModel(
        title = "春望",
        author = "杜甫",
        dynasty = "唐",
        content = "国破山河在，城春草木深。\n感时花溅泪，恨别鸟惊心。\n烽火连三月，家书抵万金。\n白头搔更短，浑欲不胜簪。",
        translation = "译文区域仅用于界面占位，后续将接入真实诗词译文与赏析数据。",
        annotation = "注释区域仅用于展示结构，后续将根据数据源拆分成注释、赏析、背景等模块。"
    )

    fun searchResults(): List<PoemSummaryUiModel> = listOf(
        PoemSummaryUiModel("春望", "杜甫", "唐", "家国忧思主题，适合作为搜索结果卡片展示。"),
        PoemSummaryUiModel("江雪", "柳宗元", "唐", "用于演示搜索结果列表与详情页跳转。"),
        PoemSummaryUiModel("饮酒", "陶渊明", "魏晋", "用于展示跨朝代查询与简要摘要。")
    )

    fun authorProfile(): AuthorProfileUiModel = AuthorProfileUiModel(
        name = "杜甫",
        dynasty = "唐",
        intro = "作者详情页当前仅保留资料展示结构，后续可接入作者简介、代表作、名句和关系推荐。"
    )

    fun representativeWorks(): List<PoemSummaryUiModel> = listOf(
        PoemSummaryUiModel("望岳", "杜甫", "唐", "会当凌绝顶，一览众山小。"),
        PoemSummaryUiModel("春夜喜雨", "杜甫", "唐", "好雨知时节，当春乃发生。"),
        PoemSummaryUiModel("登高", "杜甫", "唐", "万里悲秋常作客，百年多病独登台。")
    )
}
