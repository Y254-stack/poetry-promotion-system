package com.example.poetry.backend.tagsearch.controller;

import com.example.poetry.backend.tagsearch.dto.PoemDetailDto;
import com.example.poetry.backend.tagsearch.service.TagSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api")
public class TagSearchController {

    private final TagSearchService tagSearchService;

    public TagSearchController(TagSearchService tagSearchService) {
        this.tagSearchService = tagSearchService;
    }

    @GetMapping("/poems/{workId}")
    public PoemDetailDto getPoemDetail(@PathVariable Long workId) {
        PoemDetailDto detail = tagSearchService.getPoemDetail(workId);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Poem not found");
        }
        return detail;
    }
}
