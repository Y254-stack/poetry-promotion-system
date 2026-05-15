package com.example.poetry.backend.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.avatar")
public class AvatarStorageProperties {

    /**
     * 头像文件根目录（磁盘路径），其下会创建 avatars 子目录。
     */
    private String uploadDir = "./data/uploads";

    /**
     * 可选。若非空，则在部分接口中拼接完整 URL；数据库存相对路径 /uploads/avatars/xxx。
     */
    private String publicBaseUrl = "";

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
