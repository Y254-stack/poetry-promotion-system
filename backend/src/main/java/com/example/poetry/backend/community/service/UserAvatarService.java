package com.example.poetry.backend.community.service;

import com.example.poetry.backend.common.config.AvatarStorageProperties;
import com.example.poetry.backend.community.repository.UserProfileRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAvatarService {

    private static final long MAX_BYTES = 2 * 1024 * 1024;

    private static final Map<String, String> MIME_TO_EXT = Map.of(
        "image/jpeg", ".jpg",
        "image/png", ".png",
        "image/webp", ".webp",
        "image/gif", ".gif"
    );

    private static final Set<String> ALLOWED = MIME_TO_EXT.keySet();

    private final AvatarStorageProperties properties;
    private final UserProfileRepository userProfileRepository;

    public UserAvatarService(AvatarStorageProperties properties, UserProfileRepository userProfileRepository) {
        this.properties = properties;
        this.userProfileRepository = userProfileRepository;
    }

    public String uploadAvatar(long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择图片文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片大小不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 JPG、PNG、WebP、GIF 图片");
        }
        String ext = MIME_TO_EXT.get(contentType.toLowerCase(Locale.ROOT));
        String filename = UUID.randomUUID() + ext;

        Path root = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
        Path avatarsDir = root.resolve("avatars");
        try {
            Files.createDirectories(avatarsDir);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法创建上传目录");
        }

        Path target = avatarsDir.resolve(filename).normalize();
        if (!target.startsWith(avatarsDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法路径");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "保存文件失败");
        }

        String publicPath = "/uploads/avatars/" + filename;
        String old = userProfileRepository.getAvatarUrl(userId).orElse(null);
        userProfileRepository.updateAvatarUrl(userId, publicPath);
        deleteStoredFileIfOwned(old, root);

        if (properties.getPublicBaseUrl() != null && !properties.getPublicBaseUrl().isBlank()) {
            String base = properties.getPublicBaseUrl().trim();
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            return base + publicPath;
        }
        return publicPath;
    }

    private void deleteStoredFileIfOwned(String oldAvatarUrl, Path uploadRoot) {
        if (oldAvatarUrl == null || oldAvatarUrl.isBlank()) {
            return;
        }
        String path = oldAvatarUrl;
        int uploadsIdx = path.indexOf("/uploads/");
        if (uploadsIdx >= 0) {
            path = path.substring(uploadsIdx + "/uploads".length());
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        Path candidate = uploadRoot.resolve(path).normalize();
        if (!candidate.startsWith(uploadRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(candidate);
        } catch (IOException ignored) {
        }
    }
}
