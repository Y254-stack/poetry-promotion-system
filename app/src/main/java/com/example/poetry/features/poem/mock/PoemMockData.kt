package com.example.poetry.features.poem.mock

import com.example.poetry.features.poem.model.AuthorProfileUiModel
import com.example.poetry.features.poem.model.PoemDetailUiModel

object PoemMockData {

    fun emptyPoemDetail(): PoemDetailUiModel = PoemDetailUiModel(
        workId = 0L,
        title = "",
        author = "",
        dynasty = "",
        content = "",
        translation = "",
        annotation = "",
        appreciation = ""
    )

    fun emptyAuthorProfile(): AuthorProfileUiModel = AuthorProfileUiModel(
        name = "",
        dynasty = "",
        intro = ""
    )
}
