package com.example.poetry.backend.author.controller;

import com.example.poetry.backend.author.dto.AuthorDetailDto;
import com.example.poetry.backend.author.service.AuthorService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping("/{authorId}")
    public AuthorDetailDto getAuthorDetail(
        @PathVariable Long authorId,
        @RequestParam(defaultValue = "12") @Min(1) @Max(50) int limit
    ) {
        AuthorDetailDto detail = authorService.getAuthorDetail(authorId, limit);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found");
        }
        return detail;
    }
}
