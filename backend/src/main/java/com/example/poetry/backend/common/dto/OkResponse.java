package com.example.poetry.backend.common.dto;

public record OkResponse(boolean ok) {
    public static OkResponse success() {
        return new OkResponse(true);
    }
}

