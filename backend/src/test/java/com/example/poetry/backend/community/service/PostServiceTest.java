package com.example.poetry.backend.community.service;

import com.example.poetry.backend.community.dto.*;
import com.example.poetry.backend.community.repository.*;
import com.example.poetry.backend.user.repository.UserAccount;
import com.example.poetry.backend.user.repository.UserAuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private CommunityLikeRepository likeRepository;

    @Mock
    private CommunityCollectRepository collectRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @InjectMocks
    private PostService postService;

    // 辅助方法：创建测试用的 UserAccount
    private UserAccount createMockUser(Long userId, String nickname) {
        return new UserAccount(
                userId,
                "testuser",
                "hashedPassword",
                nickname,
                "test@example.com",
                null,
                null,
                0,
                null
        );
    }

    // 辅助方法：创建测试用的 PostDetailResponse
    private PostDetailResponse createMockPostDetail(Long postId, Long userId, int likeCount, int collectCount) {
        return new PostDetailResponse(
                postId,
                userId,
                "作者昵称",
                "帖子标题",
                "帖子内容",
                "标签",
                100,
                likeCount,
                30,
                collectCount,
                LocalDateTime.now(),
                LocalDateTime.now(),
                false,
                false
        );
    }

    // 辅助方法：创建测试用的 CommentResponse
    private CommentResponse createMockComment(Long commentId, Long postId, Long userId, String author,
                                              String content, Long parentId, Long replyUserId, String replyToAuthor,
                                              int likeCount, boolean isLiked) {
        return new CommentResponse(
                commentId, postId, userId, author, content,
                parentId, replyUserId, replyToAuthor,
                LocalDateTime.now(), likeCount, isLiked
        );
    }

    // ==================== 帖子相关测试 ====================

    @Test
    void createPost_成功_返回PostResponse() {
        Long userId = 100L;
        PostCreateRequest request = new PostCreateRequest(
                "测试标题", "这是一段测试内容", "诗词赏析", 1L, 1L
        );
        UserAccount user = createMockUser(userId, "测试昵称");

        when(userAuthRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(postRepository.createPost(eq(userId), eq(request.title()), eq(request.contentText()),
                eq(request.topicTag()), eq(request.relatedWorkId()), eq(request.relatedAuthorId())))
                .thenReturn(1L);

        PostResponse response = postService.createPost(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.postId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("测试标题");
        assertThat(response.author()).isEqualTo("测试昵称");

        verify(postRepository).createPost(anyLong(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void createPost_内容超过50字_预览被截断() {
        Long userId = 100L;
        String longContent = "这是一段超过五十个字符的测试内容，用来验证预览功能是否正确截断并添加省略号。让我们继续添加更多字符直到确实超过五十个。";
        PostCreateRequest request = new PostCreateRequest("标题", longContent, "标签", null, null);
        UserAccount user = createMockUser(userId, "昵称");

        when(userAuthRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(postRepository.createPost(anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(1L);

        PostResponse response = postService.createPost(userId, request);

        assertThat(response.preview()).endsWith("...");
        assertThat(response.preview().length()).isLessThanOrEqualTo(53);
    }

    @Test
    void createPost_用户不存在_抛出异常() {
        Long userId = 999L;
        PostCreateRequest request = new PostCreateRequest("标题", "内容", "标签", null, null);

        when(userAuthRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void getPosts_正常分页_返回列表() {
        List<PostResponse> mockPosts = List.of(
                new PostResponse(1L, 100L, "用户1", "标题1", "预览1", "标签1", 10, 5, 3, 2, LocalDateTime.now()),
                new PostResponse(2L, 101L, "用户2", "标题2", "预览2", "标签2", 8, 4, 2, 1, LocalDateTime.now())
        );

        when(postRepository.getPosts(0, 20)).thenReturn(mockPosts);
        when(postRepository.getPostCount()).thenReturn(2L);

        PostListResponse response = postService.getPosts(1, 20);

        assertThat(response.items()).hasSize(2);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.total()).isEqualTo(2);
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    void getPosts_有更多数据_hasMore为true() {
        List<PostResponse> mockPosts = List.of(
                new PostResponse(1L, 100L, "用户1", "标题1", "预览1", "标签1", 10, 5, 3, 2, LocalDateTime.now())
        );

        when(postRepository.getPosts(0, 20)).thenReturn(mockPosts);
        when(postRepository.getPostCount()).thenReturn(25L);

        PostListResponse response = postService.getPosts(1, 20);

        assertThat(response.hasMore()).isTrue();
    }

    @Test
    void getPostDetail_存在_增加浏览量并返回详情() {
        PostDetailResponse mockDetail = createMockPostDetail(1L, 100L, 50, 20);

        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(mockDetail));
        doNothing().when(postRepository).incrementViewCount(1L);

        PostDetailResponse response = postService.getPostDetail(1L);

        verify(postRepository).incrementViewCount(1L);
        assertThat(response.postId()).isEqualTo(1L);
        assertThat(response.viewCount()).isEqualTo(100);
    }

    @Test
    void getPostDetail_不存在_抛出异常() {
        when(postRepository.getPostDetail(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostDetail(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("帖子不存在");
    }

    @Test
    void getUserPosts_成功_返回用户帖子() {
        List<PostResponse> mockPosts = List.of(
                new PostResponse(1L, 100L, "用户1", "标题1", "预览1", "标签1", 10, 5, 3, 2, LocalDateTime.now())
        );

        when(postRepository.getUserPosts(eq(100L), eq(0), eq(20))).thenReturn(mockPosts);
        when(postRepository.getUserPostCount(100L)).thenReturn(1L);

        PostListResponse response = postService.getUserPosts(100L, 1, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.total()).isEqualTo(1);
    }

    @Test
    void deletePost_成功_删除所有关联数据() {
        PostDetailResponse mockPost = createMockPostDetail(1L, 100L, 50, 20);

        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(mockPost));
        doNothing().when(postRepository).deletePost(1L);

        // 修复：使用 doReturn 避免方法歧义
        doReturn(1).when(jdbcTemplate).update(anyString(), any(Map.class));

        postService.deletePost(1L, 100L);

        verify(jdbcTemplate, times(3)).update(anyString(), any(Map.class));
        verify(postRepository).deletePost(1L);
    }

    @Test
    void deletePost_非作者_抛出异常() {
        PostDetailResponse mockPost = createMockPostDetail(1L, 100L, 50, 20);

        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(mockPost));

        assertThatThrownBy(() -> postService.deletePost(1L, 200L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("无权删除");
    }

    // ==================== 评论相关测试 ====================

    @Test
    void createComment_一级评论_成功创建() {
        Long userId = 100L;
        CommentCreateRequest request = new CommentCreateRequest(1L, "这是一条评论", null, null);
        UserAccount user = createMockUser(userId, "昵称");
        PostDetailResponse post = createMockPostDetail(1L, 200L, 10, 5);
        CommentResponse newComment = createMockComment(1L, 1L, userId, "昵称", "这是一条评论", null, null, null, 0, false);

        when(userAuthRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(post));
        when(commentRepository.createComment(eq(1L), eq(userId), eq("这是一条评论"), eq(null), eq(null))).thenReturn(1L);
        when(commentRepository.getCommentById(1L)).thenReturn(newComment);
        doNothing().when(postRepository).incrementCommentCount(1L);
        doNothing().when(notificationService).createNotification(eq(200L), eq("COMMENT"), eq(userId), eq(1L), eq(1L));

        CommentResponse response = postService.createComment(userId, request);

        assertThat(response.commentId()).isEqualTo(1L);
        assertThat(response.contentText()).isEqualTo("这是一条评论");
        verify(postRepository).incrementCommentCount(1L);
    }

    @Test
    void createComment_回复评论_成功创建() {
        Long userId = 100L;
        CommentCreateRequest request = new CommentCreateRequest(1L, "回复内容", 5L, 101L);
        UserAccount user = createMockUser(userId, "昵称");
        PostDetailResponse post = createMockPostDetail(1L, 200L, 10, 5);
        CommentResponse parentComment = createMockComment(5L, 1L, 101L, "被回复者", "父评论", null, null, null, 0, false);
        CommentResponse newComment = createMockComment(2L, 1L, userId, "昵称", "回复内容", 5L, 101L, "被回复者", 0, false);

        when(userAuthRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(post));
        when(commentRepository.getCommentById(5L)).thenReturn(parentComment);
        when(commentRepository.createComment(eq(1L), eq(userId), eq("回复内容"), eq(5L), eq(101L))).thenReturn(2L);
        when(commentRepository.getCommentById(2L)).thenReturn(newComment);
        doNothing().when(postRepository).incrementCommentCount(1L);

        CommentResponse response = postService.createComment(userId, request);

        assertThat(response.parentCommentId()).isEqualTo(5L);
        assertThat(response.replyUserId()).isEqualTo(101L);
        assertThat(response.replyToAuthor()).isEqualTo("被回复者");
    }

    @Test
    void createComment_父评论不属于同一帖子_抛出异常() {
        CommentCreateRequest request = new CommentCreateRequest(1L, "回复内容", 5L, null);
        UserAccount user = createMockUser(100L, "昵称");
        PostDetailResponse post = createMockPostDetail(1L, 200L, 10, 5);
        CommentResponse parentComment = createMockComment(5L, 2L, 101L, "被回复者", "父评论", null, null, null, 0, false);

        when(userAuthRepository.findByUserId(100L)).thenReturn(Optional.of(user));
        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(post));
        when(commentRepository.getCommentById(5L)).thenReturn(parentComment);

        assertThatThrownBy(() -> postService.createComment(100L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("不属于该帖子");
    }

    @Test
    void deleteComment_删除父评论_级联删除子评论() {
        CommentResponse parentComment = createMockComment(1L, 10L, 100L, "作者", "父评论", null, null, null, 5, false);

        when(commentRepository.getCommentById(1L)).thenReturn(parentComment);
        when(commentRepository.getChildCommentCount(1L)).thenReturn(3);
        doReturn(1).when(jdbcTemplate).update(anyString(), any(Map.class));
        doNothing().when(postRepository).decrementCommentCount(anyLong());

        postService.deleteComment(100L, 1L);

        verify(jdbcTemplate, times(2)).update(anyString(), any(Map.class));
        verify(postRepository, times(4)).decrementCommentCount(10L);
    }

    @Test
    void deleteComment_删除子评论_只删除自身() {
        CommentResponse childComment = createMockComment(2L, 10L, 100L, "作者", "子评论", 1L, null, null, 2, false);

        when(commentRepository.getCommentById(2L)).thenReturn(childComment);
        doReturn(1).when(jdbcTemplate).update(anyString(), any(Map.class));
        doNothing().when(postRepository).decrementCommentCount(anyLong());

        postService.deleteComment(100L, 2L);

        verify(jdbcTemplate, times(1)).update(anyString(), any(Map.class));
        verify(postRepository).decrementCommentCount(10L);
    }

    @Test
    void deleteComment_非作者_抛出异常() {
        CommentResponse comment = createMockComment(1L, 10L, 100L, "作者", "评论", null, null, null, 5, false);

        when(commentRepository.getCommentById(1L)).thenReturn(comment);

        assertThatThrownBy(() -> postService.deleteComment(200L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("无权删除");
    }

    @Test
    void getComments_成功_返回评论列表() {
        List<CommentResponse> mockComments = List.of(
                createMockComment(1L, 1L, 100L, "用户1", "评论1", null, null, null, 5, false),
                createMockComment(2L, 1L, 101L, "用户2", "评论2", 1L, 100L, "用户1", 2, false)
        );
        PostDetailResponse post = createMockPostDetail(1L, 200L, 10, 5);

        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(post));
        when(commentRepository.getCommentsByPostId(1L)).thenReturn(mockComments);

        List<CommentResponse> response = postService.getComments(1L);

        assertThat(response).hasSize(2);
        assertThat(response.get(1).parentCommentId()).isEqualTo(1L);
    }

    @Test
    void getCommentsWithLikeStatus_已登录_返回带点赞状态() {
        List<CommentResponse> mockComments = List.of(
                createMockComment(1L, 1L, 100L, "用户1", "评论1", null, null, null, 5, false)
        );
        PostDetailResponse post = createMockPostDetail(1L, 200L, 10, 5);

        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(post));
        when(commentRepository.getCommentsByPostId(1L)).thenReturn(mockComments);
        when(commentLikeRepository.isLiked(100L, 1L)).thenReturn(true);

        List<CommentResponse> response = postService.getCommentsWithLikeStatus(1L, 100L);

        assertThat(response.get(0).isLiked()).isTrue();
    }

    // ==================== 点赞相关测试 ====================

    @Test
    void likePost_未点赞_点赞成功() {
        PostDetailResponse post = createMockPostDetail(1L, 200L, 10, 5);

        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(post));
        when(likeRepository.isLiked(100L, 1L)).thenReturn(false);
        doNothing().when(likeRepository).likePost(100L, 1L);
        doNothing().when(postRepository).incrementLikeCount(1L);
        doNothing().when(notificationService).createNotification(eq(200L), eq("LIKE"), eq(100L), eq(1L), eq(null));

        LikeResponse response = postService.likePost(100L, 1L);

        assertThat(response.success()).isTrue();
        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(11);
        assertThat(response.message()).isEqualTo("点赞成功");
    }

    @Test
    void likePost_已点赞_取消点赞() {
        PostDetailResponse post = createMockPostDetail(1L, 200L, 10, 5);

        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(post));
        when(likeRepository.isLiked(100L, 1L)).thenReturn(true);
        doNothing().when(likeRepository).unlikePost(100L, 1L);
        doNothing().when(postRepository).decrementLikeCount(1L);

        LikeResponse response = postService.likePost(100L, 1L);

        assertThat(response.success()).isTrue();
        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(9);
        assertThat(response.message()).isEqualTo("已取消点赞");
    }

    @Test
    void isPostLiked_返回点赞状态() {
        when(likeRepository.isLiked(100L, 1L)).thenReturn(true);
        boolean result = postService.isPostLiked(100L, 1L);
        assertThat(result).isTrue();
    }

    @Test
    void likeComment_未点赞_点赞成功() {
        CommentResponse comment = createMockComment(1L, 10L, 200L, "作者", "评论", null, null, null, 5, false);

        when(commentRepository.getCommentById(1L)).thenReturn(comment);
        when(commentLikeRepository.isLiked(100L, 1L)).thenReturn(false);
        doNothing().when(commentLikeRepository).likeComment(100L, 1L);

        LikeResponse response = postService.likeComment(100L, 1L);

        assertThat(response.success()).isTrue();
        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(6);
    }

    @Test
    void likeComment_已点赞_取消点赞() {
        CommentResponse comment = createMockComment(1L, 10L, 200L, "作者", "评论", null, null, null, 5, false);

        when(commentRepository.getCommentById(1L)).thenReturn(comment);
        when(commentLikeRepository.isLiked(100L, 1L)).thenReturn(true);
        doNothing().when(commentLikeRepository).unlikeComment(100L, 1L);

        LikeResponse response = postService.likeComment(100L, 1L);

        assertThat(response.success()).isTrue();
        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(4);
    }

    @Test
    void isCommentLiked_返回点赞状态() {
        when(commentLikeRepository.isLiked(100L, 1L)).thenReturn(true);
        boolean result = postService.isCommentLiked(100L, 1L);
        assertThat(result).isTrue();
    }

    // ==================== 收藏相关测试 ====================

    @Test
    void collectPost_未收藏_收藏成功() {
        PostDetailResponse post = createMockPostDetail(1L, 200L, 10, 2);

        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(post));
        when(collectRepository.isCollected(100L, 1L)).thenReturn(false);
        doNothing().when(collectRepository).collectPost(100L, 1L);
        doNothing().when(postRepository).incrementCollectCount(1L);
        doNothing().when(notificationService).createNotification(eq(200L), eq("COLLECT"), eq(100L), eq(1L), eq(null));

        CollectResponse response = postService.collectPost(100L, 1L);

        assertThat(response.success()).isTrue();
        assertThat(response.isCollected()).isTrue();
        assertThat(response.collectCount()).isEqualTo(3);
        assertThat(response.message()).isEqualTo("收藏成功");
    }

    @Test
    void collectPost_已收藏_取消收藏() {
        PostDetailResponse post = createMockPostDetail(1L, 200L, 10, 2);

        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(post));
        when(collectRepository.isCollected(100L, 1L)).thenReturn(true);
        doNothing().when(collectRepository).uncollectPost(100L, 1L);
        doNothing().when(postRepository).decrementCollectCount(1L);

        CollectResponse response = postService.collectPost(100L, 1L);

        assertThat(response.success()).isTrue();
        assertThat(response.isCollected()).isFalse();
        assertThat(response.collectCount()).isEqualTo(1);
        assertThat(response.message()).isEqualTo("已取消收藏");
    }

    @Test
    void isPostCollected_返回收藏状态() {
        when(collectRepository.isCollected(100L, 1L)).thenReturn(true);
        boolean result = postService.isPostCollected(100L, 1L);
        assertThat(result).isTrue();
    }

    // ==================== 喜欢帖子列表测试 ====================

    @Test
    void getLikedPosts_成功_返回喜欢帖子列表() {
        List<PostResponse> mockPosts = List.of(
                new PostResponse(1L, 100L, "用户1", "标题1", "预览1", "标签1", 10, 5, 3, 2, LocalDateTime.now())
        );

        when(likeRepository.getLikedPosts(eq(100L), eq(0), eq(20))).thenReturn(mockPosts);
        when(likeRepository.getLikedPostCount(100L)).thenReturn(1L);

        LikedPostsResponse response = postService.getLikedPosts(100L, 1, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.page()).isEqualTo(1);
    }
}