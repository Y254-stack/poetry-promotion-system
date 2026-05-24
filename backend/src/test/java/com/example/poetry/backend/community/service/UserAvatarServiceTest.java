package com.example.poetry.backend.community.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.poetry.backend.common.config.AvatarStorageProperties;
import com.example.poetry.backend.community.repository.UserProfileRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserAvatarServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private MultipartFile file;

    @Mock
    private UserProfileRepository userProfileRepository;

    private AvatarStorageProperties properties;
    private UserAvatarService userAvatarService;

    @BeforeEach
    void setUp() {
        properties = new AvatarStorageProperties();
        properties.setUploadDir(tempDir.toString());
        properties.setPublicBaseUrl("http://localhost:8080");
        userAvatarService = new UserAvatarService(properties, userProfileRepository);
    }

    @Test
    void uploadAvatar_emptyFile_throwsBadRequest() {
        when(file.isEmpty()).thenReturn(true);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> userAvatarService.uploadAvatar(1L, file)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("请选择图片文件", ex.getReason());
    }

    @Test
    void uploadAvatar_fileTooLarge_throwsBadRequest() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(3L * 1024 * 1024);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> userAvatarService.uploadAvatar(1L, file)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("图片大小不能超过 2MB", ex.getReason());
    }

    @Test
    void uploadAvatar_invalidMime_throwsBadRequest() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("text/plain");

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> userAvatarService.uploadAvatar(1L, file)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("仅支持 JPG、PNG、WebP、GIF 图片", ex.getReason());
    }

    @Test
    void uploadAvatar_success_writesFileAndUpdatesDb() throws IOException {
        byte[] content = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) content.length);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        when(userProfileRepository.getAvatarUrl(7L)).thenReturn(java.util.Optional.empty());

        String url = userAvatarService.uploadAvatar(7L, file);

        assertTrue(url.startsWith("http://localhost:8080/uploads/avatars/"));
        assertTrue(url.endsWith(".jpg"));
        verify(userProfileRepository).updateAvatarUrl(eq(7L), org.mockito.ArgumentMatchers.startsWith("/uploads/avatars/"));

        Path avatarsDir = tempDir.resolve("avatars");
        assertTrue(Files.exists(avatarsDir));
        assertEquals(1L, Files.list(avatarsDir).count());
    }

    @Test
    void uploadAvatar_nullFile_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> userAvatarService.uploadAvatar(1L, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userProfileRepository, never()).updateAvatarUrl(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }
}
