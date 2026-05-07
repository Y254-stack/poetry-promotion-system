package com.example.poetry.backend.community.service;

import com.example.poetry.backend.community.dto.CommentCreateRequest;
import com.example.poetry.backend.community.dto.CommentResponse;
import com.example.poetry.backend.community.dto.PostCreateRequest;
import com.example.poetry.backend.community.dto.PostDetailResponse;
import com.example.poetry.backend.community.dto.PostListResponse;
import com.example.poetry.backend.community.dto.PostResponse;
import com.example.poetry.backend.community.repository.CommentRepository;
import com.example.poetry.backend.community.repository.PostRepository;
import com.example.poetry.backend.user.repository.UserAccount;
import com.example.poetry.backend.user.repository.UserAuthRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserAuthRepository userAuthRepository;

    public PostService(PostRepository postRepository,
                       CommentRepository commentRepository,
                       UserAuthRepository userAuthRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userAuthRepository = userAuthRepository;
    }

    /**
     * 创建帖子
     */
    @Transactional
    public PostResponse createPost(Long userId, PostCreateRequest request) {
        // 验证用户是否存在
        UserAccount user = userAuthRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));

        long postId = postRepository.createPost(
                userId,
                request.title(),
                request.contentText(),
                request.topicTag()
        );

        // 返回创建的帖子信息
        String preview = request.contentText().length() > 50
                ? request.contentText().substring(0, 50) + "..."
                : request.contentText();

        return new PostResponse(
                postId,
                userId,
                user.nickname() != null ? user.nickname() : user.username(),
                request.title(),
                preview,
                request.topicTag(),
                0, 0, 0, 0,
                java.time.LocalDateTime.now()
        );
    }

    /**
     * 获取帖子列表（分页）
     */
    public PostListResponse getPosts(int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;

        int offset = (page - 1) * pageSize;
        List<PostResponse> items = postRepository.getPosts(offset, pageSize);
        long total = postRepository.getPostCount();
        boolean hasMore = (long) offset + pageSize < total;

        return new PostListResponse(items, page, pageSize, total, hasMore);
    }

    /**
     * 获取帖子详情
     */
    @Transactional
    public PostDetailResponse getPostDetail(Long postId) {
        // 增加浏览量
        postRepository.incrementViewCount(postId);

        return postRepository.getPostDetail(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在"));
    }

    /**
     * 发布评论（支持一级评论和回复）
     */
    @Transactional
    public CommentResponse createComment(Long userId, CommentCreateRequest request) {
        // 验证用户
        UserAccount user = userAuthRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));

        // 验证帖子是否存在
        postRepository.getPostDetail(request.postId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在"));

        // 如果是回复评论，验证父评论是否存在
        if (request.parentCommentId() != null) {
            CommentResponse parentComment = commentRepository.getCommentById(request.parentCommentId());
            if (parentComment == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "被回复的评论不存在");
            }
            // 确保父评论属于同一个帖子
            if (!parentComment.postId().equals(request.postId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回复的评论不属于该帖子");
            }
        }

        long commentId = commentRepository.createComment(
                request.postId(),
                userId,
                request.contentText(),
                request.parentCommentId(),
                request.replyUserId()
        );

        // 增加帖子的评论数
        postRepository.incrementCommentCount(request.postId());

        // 获取刚创建的评论详情
        CommentResponse newComment = commentRepository.getCommentById(commentId);
        if (newComment == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "创建评论失败");
        }

        return newComment;
    }

    /**
     * 获取帖子的评论列表（已按树形结构组织）
     * 返回的列表中是扁平结构，但已按父子顺序排序，UI层可以根据parentCommentId来缩进显示
     */
    public List<CommentResponse> getComments(Long postId) {
        // 验证帖子是否存在
        postRepository.getPostDetail(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在"));

        return commentRepository.getCommentsByPostId(postId);
    }
}