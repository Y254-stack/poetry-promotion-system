package com.example.poetry.features.home.mock

import com.example.poetry.features.home.model.CategoryEntryUiModel
import com.example.poetry.features.home.model.CategoryType

object HomeMockData {

    fun categoryEntries(): List<CategoryEntryUiModel> = listOf(
        CategoryEntryUiModel(
            CategoryType.DYNASTY,
            "\u671d\u4ee3\u5206\u7c7b",
            "\u6309\u671d\u4ee3\u6d4f\u89c8\u8bd7\u8bcd"
        ),
        CategoryEntryUiModel(
            CategoryType.AUTHOR,
            "\u4f5c\u8005\u5206\u7c7b",
            "\u6309\u8bd7\u4eba\u6d4f\u89c8\u4f5c\u54c1"
        ),
        CategoryEntryUiModel(
            CategoryType.COLLECTION,
            "\u8bd7\u96c6\u5206\u7c7b",
            "\u6309\u8bd7\u96c6\u6d4f\u89c8\u4f5c\u54c1"
        )
    )
}
