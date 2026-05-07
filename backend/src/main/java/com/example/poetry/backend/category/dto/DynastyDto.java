package com.example.poetry.backend.category.dto;

public class DynastyDto {
    private Long dynastyId;
    private String dynastyName;

    public DynastyDto() {
    }

    public DynastyDto(Long dynastyId, String dynastyName) {
        this.dynastyId = dynastyId;
        this.dynastyName = dynastyName;
    }

    public Long getDynastyId() {
        return dynastyId;
    }

    public void setDynastyId(Long dynastyId) {
        this.dynastyId = dynastyId;
    }

    public String getDynastyName() {
        return dynastyName;
    }

    public void setDynastyName(String dynastyName) {
        this.dynastyName = dynastyName;
    }
}
