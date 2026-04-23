package com.example.poetry.backend.tagsearch.dto;

import java.util.List;

public record PoemSearchItemDto(
    Long workId,
    String title,
    String authorName,
    String dynastyName,
    String contentPreview,
    List<String> matchedTags,
    Double hotScore,
    String publishTime
) {
}

