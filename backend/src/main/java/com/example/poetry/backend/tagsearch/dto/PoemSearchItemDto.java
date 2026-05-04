package com.example.poetry.backend.tagsearch.dto;

public record PoemSearchItemDto(
    Long workId,
    String title,
    String authorName,
    String dynastyName,
    String contentPreview,
    String matchedTags,
    Integer hotScore,
    String publishTime
) {
}
