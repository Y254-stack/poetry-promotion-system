package com.example.poetry.backend.tagsearch.dto;

public record PoemDetailDto(
    Long workId,
    String title,
    String authorName,
    String dynastyName,
    String contentText,
    String translationText,
    String annotationText,
    String appreciationText
) {
}
