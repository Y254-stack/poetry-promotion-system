package com.example.poetry.backend.category.controller;

import com.example.poetry.backend.category.dto.AuthorDto;
import com.example.poetry.backend.category.dto.DynastyDto;
import com.example.poetry.backend.category.repository.CategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/dynasties")
    public List<DynastyDto> getDynasties() {
        return categoryRepository.getAllDynasties();
    }

    @GetMapping("/authors")
    public List<AuthorDto> getAuthors() {
        return categoryRepository.getAllAuthors();
    }
}
