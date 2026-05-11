package com.example.poetry.backend.community.service;

import com.example.poetry.backend.community.dto.*;
import com.example.poetry.backend.community.repository.CommentLikeRepository;
import com.example.poetry.backend.community.repository.CommentRepository;
import com.example.poetry.backend.community.repository.CommunityCollectRepository;
import com.example.poetry.backend.community.repository.CommunityLikeRepository;
import com.example.poetry.backend.community.repository.PostRepository;
import com.example.poetry.backend.user.repository.UserAccount;
import com.example.poetry.backend.user.repository.UserAuthRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommunityLikeRepository likeRepository;
    private final CommunityCollectRepository collectRepository;
    private final NotificationService notificationService;
    private final UserAuthRepository userAuthRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostService(PostRepository postRepository,
                       CommentRepository commentRepository,
                       CommentLikeRepository commentLikeRepository,
                       CommunityLikeRepository likeRepository,
                       CommunityCollectRepository collectRepository,
                       NotificationService notificationService,
                       UserAuthRepository userAuthRepository,
                       NamedParameterJdbcTemplate jdbcTemplate) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.likeRepository = likeRepository;
        this.collectRepository = collectRepository;
        this.notificationService = notificationService;
        this.userAuthRepository = userAuthRepository;
        this.jdbcTemplate = jdbcTemplate;
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

        // 发送通知给帖子作者
        PostDetailResponse post = postRepository.getPostDetail(request.postId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在"));
        notificationService.createNotification(post.userId(), "COMMENT", userId, request.postId(), commentId);

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

    /**
     * 获取帖子的评论列表（包含当前用户点赞状态）
     */
    public List<CommentResponse> getCommentsWithLikeStatus(Long postId, Long userId) {
        // 验证帖子是否存在
        postRepository.getPostDetail(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在"));

        List<CommentResponse> comments = commentRepository.getCommentsByPostId(postId);

        // 设置每个评论的点赞状态
        return comments.stream()
                .map(comment -> new CommentResponse(
                        comment.commentId(),
                        comment.postId(),
                        comment.userId(),
                        comment.author(),
                        comment.contentText(),
                        comment.parentCommentId(),
                        comment.replyUserId(),
                        comment.replyToAuthor(),
                        comment.createdAt(),
                        comment.likeCount(),
                        userId != null ? commentLikeRepository.isLiked(userId, comment.commentId()) : false
                ))
                .toList();
    }

    /**
     * 删除评论（支持级联删除）
     * 删除父评论时，下面的子评论也一起被删除
     * 删除子评论时，只有该评论被删除
     */
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        // 验证评论是否存在
        CommentResponse comment = commentRepository.getCommentById(commentId);
        if (comment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在");
        }

        // 验证用户是否为评论发布者
        if (!comment.userId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除该评论");
        }

        // 获取被删除的评论所属的帖子ID
        Long postId = comment.postId();

        if (comment.parentCommentId() == null) {
            // 这是父评论，需要级联删除所有子评论
            // 先统计子评论数量
            int childCount = commentRepository.getChildCommentCount(commentId);

            // 软删除父评论和所有子评论
            String deleteParentSql = "UPDATE community_comment SET status = 'DELETED' WHERE comment_id = :commentId";
            jdbcTemplate.update(deleteParentSql, new MapSqlParameterSource("commentId", commentId));

            String deleteChildrenSql = "UPDATE community_comment SET status = 'DELETED' WHERE parent_comment_id = :parentCommentId";
            jdbcTemplate.update(deleteChildrenSql, new MapSqlParameterSource("parentCommentId", commentId));

            // 减少帖子的评论数（父评论 + 子评论数量）
            for (int i = 0; i <= childCount; i++) {
                postRepository.decrementCommentCount(postId);
            }
        } else {
            // 这是子评论，只删除该评论
            String sql = "UPDATE community_comment SET status = 'DELETED' WHERE comment_id = :commentId";
            jdbcTemplate.update(sql, new MapSqlParameterSource("commentId", commentId));

            // 减少帖子的评论数
            postRepository.decrementCommentCount(postId);
        }
    }

    /**
     * 点赞帖子
     */
    @Transactional
    public LikeResponse likePost(Long userId, Long postId) {
        // 验证帖子是否存在
        PostDetailResponse post = postRepository.getPostDetail(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在"));

        boolean wasLiked = likeRepository.isLiked(userId, postId);

        if (wasLiked) {
            // 取消点赞
            likeRepository.unlikePost(userId, postId);
            postRepository.decrementLikeCount(postId);
            return new LikeResponse(true, false, post.likeCount() - 1, "已取消点赞");
        } else {
            // 点赞
            likeRepository.likePost(userId, postId);
            postRepository.incrementLikeCount(postId);

            // 发送通知给帖子作者
            notificationService.createNotification(post.userId(), "LIKE", userId, postId, null);

            return new LikeResponse(true, true, post.likeCount() + 1, "点赞成功");
        }
    }

    /**
     * 检查帖子是否已点赞
     */
    public boolean isPostLiked(Long userId, Long postId) {
        return likeRepository.isLiked(userId, postId);
    }

    /**
     * 点赞/取消点赞评论
     */
    @Transactional
    public LikeResponse likeComment(Long userId, Long commentId) {
        // 验证评论是否存在
        CommentResponse comment = commentRepository.getCommentById(commentId);
        if (comment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在");
        }

        boolean wasLiked = commentLikeRepository.isLiked(userId, commentId);
        int currentLikeCount = comment.likeCount() != null ? comment.likeCount() : 0;

        if (wasLiked) {
            // 取消点赞
            commentLikeRepository.unlikeComment(userId, commentId);
            return new LikeResponse(true, false, currentLikeCount - 1, "已取消点赞");
        } else {
            // 点赞
            commentLikeRepository.likeComment(userId, commentId);
            return new LikeResponse(true, true, currentLikeCount + 1, "点赞成功");
        }
    }

    /**
     * 检查评论是否已点赞
     */
    public boolean isCommentLiked(Long userId, Long commentId) {
        return commentLikeRepository.isLiked(userId, commentId);
    }

    /**
     * 获取用户喜欢的帖子列表
     */
    public LikedPostsResponse getLikedPosts(Long userId, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;

        int offset = (page - 1) * pageSize;
        List<PostResponse> items = likeRepository.getLikedPosts(userId, offset, pageSize);
        long total = likeRepository.getLikedPostCount(userId);
        boolean hasMore = (long) offset + pageSize < total;

        return new LikedPostsResponse(items, page, pageSize, total, hasMore);
    }

    /**
     * 收藏帖子
     */
    @Transactional
    public CollectResponse collectPost(Long userId, Long postId) {
        // 验证帖子是否存在
        PostDetailResponse post = postRepository.getPostDetail(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在"));

        boolean wasCollected = collectRepository.isCollected(userId, postId);

        if (wasCollected) {
            // 取消收藏
            collectRepository.uncollectPost(userId, postId);
            postRepository.decrementCollectCount(postId);
            return new CollectResponse(true, false, post.collectCount() - 1, "已取消收藏");
        } else {
            // 收藏
            collectRepository.collectPost(userId, postId);
            postRepository.incrementCollectCount(postId);

            // 发送通知给帖子作者
            notificationService.createNotification(post.userId(), "COLLECT", userId, postId, null);

            return new CollectResponse(true, true, post.collectCount() + 1, "收藏成功");
        }
    }

    /**
     * 检查帖子是否已收藏
     */
    public boolean isPostCollected(Long userId, Long postId) {
        return collectRepository.isCollected(userId, postId);
    }
}