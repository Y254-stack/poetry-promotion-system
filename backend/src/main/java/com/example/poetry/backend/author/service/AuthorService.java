package com.example.poetry.backend.author.service;

import com.example.poetry.backend.author.dto.AuthorDetailDto;
import com.example.poetry.backend.author.repository.AuthorRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public AuthorDetailDto getAuthorDetail(Long authorId, int workLimit) {
        return authorRepository.findAuthorDetail(authorId, workLimit);
    }
}
