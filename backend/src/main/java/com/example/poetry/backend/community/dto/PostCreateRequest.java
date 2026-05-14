package com.example.poetry.backend.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 255, message = "标题最多255个字符")
        String title,

        @NotBlank(message = "内容不能为空")
        String contentText,

        @Size(max = 64, message = "标签最多64个字符")
        String topicTag
) {
}