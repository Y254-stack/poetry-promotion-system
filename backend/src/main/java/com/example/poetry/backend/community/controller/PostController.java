package com.example.poetry.backend.community.controller;

import com.example.poetry.backend.community.dto.CollectResponse;
import com.example.poetry.backend.community.dto.*;
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
     * 获取帖子的评论列表（不含点赞状态）
     */
    @GetMapping("/comments/{postId}")
    public List<CommentResponse> getComments(@PathVariable Long postId) {
        return postService.getComments(postId);
    }

    /**
     * 获取帖子的评论列表（包含当前用户点赞状态）
     */
    @GetMapping("/comments/{postId}/with-likes")
    public List<CommentResponse> getCommentsWithLikes(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long postId
    ) {
        Long userId = null;
        try {
            userId = extractUserId(authorization);
        } catch (ResponseStatusException e) {
            // 用户未登录，返回不带点赞状态的列表
        }
        return postService.getCommentsWithLikeStatus(postId, userId);
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comment/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long commentId
    ) {
        Long userId = extractUserId(authorization);
        postService.deleteComment(userId, commentId);
    }

    /**
     * 点赞/取消点赞帖子
     */
    @PostMapping("/post/{postId}/like")
    public LikeResponse likePost(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long postId
    ) {
        Long userId = extractUserId(authorization);
        return postService.likePost(userId, postId);
    }

    /**
     * 检查帖子是否已点赞
     */
    @GetMapping("/post/{postId}/is-liked")
    public boolean isPostLiked(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long postId
    ) {
        Long userId = extractUserId(authorization);
        return postService.isPostLiked(userId, postId);
    }

    /**
     * 点赞/取消点赞评论
     */
    @PostMapping("/comment/{commentId}/like")
    public LikeResponse likeComment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long commentId
    ) {
        Long userId = extractUserId(authorization);
        return postService.likeComment(userId, commentId);
    }

    /**
     * 检查评论是否已点赞
     */
    @GetMapping("/comment/{commentId}/is-liked")
    public boolean isCommentLiked(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long commentId
    ) {
        Long userId = extractUserId(authorization);
        return postService.isCommentLiked(userId, commentId);
    }

    /**
     * 获取用户喜欢的帖子列表
     */
    @GetMapping("/posts/liked")
    public LikedPostsResponse getLikedPosts(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        Long userId = extractUserId(authorization);
        return postService.getLikedPosts(userId, page, pageSize);
    }

    /**
     * 收藏/取消收藏帖子
     */
    @PostMapping("/post/{postId}/collect")
    public CollectResponse collectPost(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long postId
    ) {
        Long userId = extractUserId(authorization);
        return postService.collectPost(userId, postId);
    }

    /**
     * 检查帖子是否已收藏
     */
    @GetMapping("/post/{postId}/is-collected")
    public boolean isPostCollected(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long postId
    ) {
        Long userId = extractUserId(authorization);
        return postService.isPostCollected(userId, postId);
    }

    private Long extractUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 Bearer Token");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return jwtTokenProvider.parseUserId(token);
    }
}