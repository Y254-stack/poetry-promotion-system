package com.example.poetry.backend.community.controller;

import com.example.poetry.backend.community.dto.CommentCreateRequest;
import com.example.poetry.backend.community.dto.CommentResponse;
import com.example.poetry.backend.community.dto.PostCreateRequest;
import com.example.poetry.backend.community.dto.PostDetailResponse;
import com.example.poetry.backend.community.dto.PostListResponse;
import com.example.poetry.backend.community.dto.PostResponse;
import com.example.poetry.backend.community.service.PostService;
import com.example.poetry.backend.user.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/community")
public class PostController {

    private final PostService postService;
    private final JwtTokenProvider jwtTokenProvider;

    public PostController(PostService postService, JwtTokenProvider jwtTokenProvider) {
        this.postService = postService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 创建帖子
     */
    @PostMapping("/post")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody PostCreateRequest request
    ) {
        Long userId = extractUserId(authorization);
        return postService.createPost(userId, request);
    }

    /**
     * 获取帖子列表（分页）
     */
    @GetMapping("/posts")
    public PostListResponse getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return postService.getPosts(page, pageSize);
    }

    /**
     * 获取帖子详情
     */
    @GetMapping("/post/{postId}")
    public PostDetailResponse getPostDetail(@PathVariable Long postId) {
        return postService.getPostDetail(postId);
    }

    /**
     * 获取指定用户的帖子列表
     */
    @GetMapping("/user/posts")
    public PostListResponse getUserPosts(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return postService.getUserPosts(userId, page, pageSize);
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/post/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long postId
    ) {
        Long userId = extractUserId(authorization);
        postService.deletePost(postId, userId);
    }

    /**
     * 发布评论
     */
    @PostMapping("/comment")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        Long userId = extractUserId(authorization);
        return postService.createComment(userId, request);
    }

    /**
     * 获取帖子的评论列表
     */
    @GetMapping("/comments/{postId}")
    public List<CommentResponse> getComments(@PathVariable Long postId) {
        return postService.getComments(postId);
    }

    private Long extractUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 Bearer Token");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return jwtTokenProvider.parseUserId(token);
    }
}