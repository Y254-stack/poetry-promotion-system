package com.example.poetry.backend.author.dto;

import com.example.poetry.backend.tagsearch.dto.PoemSearchItemDto;
import java.util.List;

public record AuthorDetailDto(
    Long authorId,
    String authorName,
    String dynastyName,
    String introText,
    Integer workCount,
    List<PoemSearchItemDto> works
) {
}
