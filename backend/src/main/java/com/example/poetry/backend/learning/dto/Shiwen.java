package com.example.poetry.backend.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shiwen {
    private String id;
    private String href;
    private String title;
    private String author;
    private String dynasty;
    private String content;  // 整首诗，带 <br/>
    private String sons;
    private String links;
}