package com.example.poetry.backend.tagsearch.dto;

public record TagDto(
    Long tagId,
    String tagName,
    String tagType,
    Integer workCount
) {
}

