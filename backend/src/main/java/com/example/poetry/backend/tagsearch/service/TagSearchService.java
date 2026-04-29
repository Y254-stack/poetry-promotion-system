package com.example.poetry.backend.tagsearch.service;

import com.example.poetry.backend.tagsearch.dto.PoemDetailDto;
import com.example.poetry.backend.tagsearch.repository.TagSearchRepository;
import org.springframework.stereotype.Service;

@Service
public class TagSearchService {

    private final TagSearchRepository repository;

    public TagSearchService(TagSearchRepository repository) {
        this.repository = repository;
    }

    public PoemDetailDto getPoemDetail(Long workId) {
        return repository.findPoemDetail(workId);
    }
}
