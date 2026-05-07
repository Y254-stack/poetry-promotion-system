package com.example.poetry.backend.category.dto;

public class AuthorDto {
    private Long authorId;
    private String authorName;
    private String dynastyName;

    public AuthorDto() {
    }

    public AuthorDto(Long authorId, String authorName, String dynastyName) {
        this.authorId = authorId;
        this.authorName = authorName;
        this.dynastyName = dynastyName;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getDynastyName() {
        return dynastyName;
    }

    public void setDynastyName(String dynastyName) {
        this.dynastyName = dynastyName;
    }
}
