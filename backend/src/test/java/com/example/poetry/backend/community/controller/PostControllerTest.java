package com.example.poetry.backend.community.controller;

import com.example.poetry.backend.community.dto.*;
import com.example.poetry.backend.community.service.PostService;
import com.example.poetry.backend.user.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private PostController postController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
    }

    @Test
    void createPost_成功_返回201() throws Exception {
        PostCreateRequest request = new PostCreateRequest("测试标题", "测试内容内容", "诗词赏析", 1L, 1L);
        PostResponse response = new PostResponse(
                1L, 100L, "测试用户", "测试标题", "测试内容预览",
                "诗词赏析", 0, 0, 0, 0, LocalDateTime.now()
        );

        when(jwtTokenProvider.parseUserId(anyString())).thenReturn(100L);
        when(postService.createPost(eq(100L), any(PostCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/community/post")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(1L))
                .andExpect(jsonPath("$.title").value("测试标题"))
                .andExpect(jsonPath("$.author").value("测试用户"));
    }

    @Test
    void createPost_无Token_返回401() throws Exception {
        PostCreateRequest request = new PostCreateRequest("标题", "内容", "标签", null, null);

        mockMvc.perform(post("/api/community/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPosts_默认参数_返回帖子列表() throws Exception {
        List<PostResponse> posts = List.of(
                new PostResponse(1L, 100L, "用户1", "标题1", "预览1", "标签1", 10, 5, 3, 2, LocalDateTime.now()),
                new PostResponse(2L, 101L, "用户2", "标题2", "预览2", "标签2", 8, 4, 2, 1, LocalDateTime.now())
        );
        PostListResponse response = new PostListResponse(posts, 1, 20, 2, false);

        when(postService.getPosts(1, 20)).thenReturn(response);

        mockMvc.perform(get("/api/community/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void getPosts_自定义分页_返回指定页() throws Exception {
        List<PostResponse> posts = List.of(
                new PostResponse(1L, 100L, "用户1", "标题1", "预览1", "标签1", 10, 5, 3, 2, LocalDateTime.now())
        );
        PostListResponse response = new PostListResponse(posts, 2, 10, 15, true);

        when(postService.getPosts(2, 10)).thenReturn(response);

        mockMvc.perform(get("/api/community/posts?page=2&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    void getPostDetail_存在_返回详情() throws Exception {
        PostDetailResponse response = new PostDetailResponse(
                1L, 100L, "用户名", "标题", "完整内容",
                "标签", 100, 50, 30, 20,
                LocalDateTime.now(), LocalDateTime.now(), true, true
        );

        when(postService.getPostDetail(1L)).thenReturn(response);

        mockMvc.perform(get("/api/community/post/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(1L))
                .andExpect(jsonPath("$.title").value("标题"))
                .andExpect(jsonPath("$.viewCount").value(100));
    }

    @Test
    void getUserPosts_成功_返回用户帖子列表() throws Exception {
        List<PostResponse> posts = List.of(
                new PostResponse(1L, 100L, "用户1", "标题1", "预览1", "标签1", 10, 5, 3, 2, LocalDateTime.now())
        );
        PostListResponse response = new PostListResponse(posts, 1, 20, 1, false);

        when(postService.getUserPosts(eq(100L), eq(1), eq(20))).thenReturn(response);

        mockMvc.perform(get("/api/community/user/posts?userId=100&page=1&pageSize=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void deletePost_成功_返回204() throws Exception {
        when(jwtTokenProvider.parseUserId(anyString())).thenReturn(100L);
        doNothing().when(postService).deletePost(1L, 100L);

        mockMvc.perform(delete("/api/community/post/1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void createComment_成功_返回201() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest(1L, "评论内容", null, null);
        CommentResponse response = new CommentResponse(
                1L, 1L, 100L, "用户名", "评论内容",
                null, null, null, LocalDateTime.now(), 0, false
        );

        when(jwtTokenProvider.parseUserId(anyString())).thenReturn(100L);
        when(postService.createComment(eq(100L), any(CommentCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/community/comment")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value(1L))
                .andExpect(jsonPath("$.contentText").value("评论内容"));
    }

    @Test
    void getComments_成功_返回评论列表() throws Exception {
        List<CommentResponse> comments = List.of(
                new CommentResponse(1L, 1L, 100L, "用户1", "评论1", null, null, null, LocalDateTime.now(), 5, false),
                new CommentResponse(2L, 1L, 101L, "用户2", "评论2", 1L, 100L, "用户1", LocalDateTime.now(), 2, false)
        );

        when(postService.getComments(1L)).thenReturn(comments);

        mockMvc.perform(get("/api/community/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].commentId").value(1L))
                .andExpect(jsonPath("$[1].parentCommentId").value(1L));
    }

    @Test
    void getCommentsWithLikes_已登录_返回带点赞状态的评论() throws Exception {
        List<CommentResponse> comments = List.of(
                new CommentResponse(1L, 1L, 100L, "用户1", "评论1", null, null, null, LocalDateTime.now(), 5, true)
        );

        when(jwtTokenProvider.parseUserId(anyString())).thenReturn(100L);
        when(postService.getCommentsWithLikeStatus(eq(1L), eq(100L))).thenReturn(comments);

        mockMvc.perform(get("/api/community/comments/1/with-likes")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isLiked").value(true));
    }

    @Test
    void deleteComment_成功_返回204() throws Exception {
        when(jwtTokenProvider.parseUserId(anyString())).thenReturn(100L);
        doNothing().when(postService).deleteComment(100L, 1L);

        mockMvc.perform(delete("/api/community/comment/1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void likePost_点赞成功_返回LikeResponse() throws Exception {
        LikeResponse response = new LikeResponse(true, true, 11, "点赞成功");

        when(jwtTokenProvider.parseUserId(anyString())).thenReturn(100L);
        when(postService.likePost(100L, 1L)).thenReturn(response);

        mockMvc.perform(post("/api/community/post/1/like")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isLiked").value(true))
                .andExpect(jsonPath("$.likeCount").value(11));
    }

    @Test
    void isPostLiked_已点赞_返回true() throws Exception {
        when(jwtTokenProvider.parseUserId(anyString())).thenReturn(100L);
        when(postService.isPostLiked(100L, 1L)).thenReturn(true);

        mockMvc.perform(get("/api/community/post/1/is-liked")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void likeComment_点赞成功_返回LikeResponse() throws Exception {
        LikeResponse response = new LikeResponse(true, true, 6, "点赞成功");

        when(jwtTokenProvider.parseUserId(anyString())).thenReturn(100L);
        when(postService.likeComment(100L, 1L)).thenReturn(response);

        mockMvc.perform(post("/api/community/comment/1/like")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLiked").value(true));
    }

    @Test
    void getLikedPosts_成功_返回喜欢帖子列表() throws Exception {
        List<PostResponse> posts = List.of(
                new PostResponse(1L, 100L, "用户1", "标题1", "预览1", "标签1", 10, 5, 3, 2, LocalDateTime.now())
        );
        LikedPostsResponse response = new LikedPostsResponse(posts, 1, 20, 1, false);

        when(jwtTokenProvider.parseUserId(anyString())).thenReturn(100L);
        when(postService.getLikedPosts(eq(100L), eq(1), eq(20))).thenReturn(response);

        mockMvc.perform(get("/api/community/posts/liked")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void collectPost_收藏成功_返回CollectResponse() throws Exception {
        CollectResponse response = new CollectResponse(true, true, 5, "收藏成功");

        when(jwtTokenProvider.parseUserId(anyString())).thenReturn(100L);
        when(postService.collectPost(100L, 1L)).thenReturn(response);

        mockMvc.perform(post("/api/community/post/1/collect")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCollected").value(true))
                .andExpect(jsonPath("$.collectCount").value(5));
    }

    @Test
    void isPostCollected_已收藏_返回true() throws Exception {
        when(jwtTokenProvider.parseUserId(anyString())).thenReturn(100L);
        when(postService.isPostCollected(100L, 1L)).thenReturn(true);

        mockMvc.perform(get("/api/community/post/1/is-collected")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}