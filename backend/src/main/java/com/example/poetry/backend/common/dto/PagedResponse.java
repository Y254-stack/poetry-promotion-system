package com.example.poetry.backend.common.dto;

import java.util.List;

public record PagedResponse<T>(
    int page,
    int pageSize,
    int total,
    List<T> items
) {
}

