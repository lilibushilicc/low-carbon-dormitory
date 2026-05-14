package com.example.lowcarbondormitory.service.admin;

import com.example.lowcarbondormitory.dto.response.AdminRewardImageUploadResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminRewardImageStorageService {

    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Value("${app.upload.reward-dir}")
    private String rewardUploadDir;

    @Value("${app.upload.reward-url-prefix}")
    private String rewardUrlPrefix;

    public AdminRewardImageUploadResponse store(MultipartFile file) {
        validateFile(file);

        String extension = resolveExtension(file);
        String folder = LocalDate.now().format(MONTH_FORMATTER);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetDirectory = Path.of(rewardUploadDir).toAbsolutePath().normalize().resolve(folder);
        Path targetFile = targetDirectory.resolve(fileName).normalize();

        try {
            Files.createDirectories(targetDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("图片上传失败");
        }

        AdminRewardImageUploadResponse response = new AdminRewardImageUploadResponse();
        response.setImageUrl(buildImageUrl(folder, fileName));
        response.setOriginalName(file.getOriginalFilename());
        response.setSize(file.getSize());
        return response;
    }

    public Path getRootDirectory() {
        return Path.of(rewardUploadDir).toAbsolutePath().normalize();
    }

    public String getResourcePattern() {
        return normalizeUrlPrefix() + "/**";
    }

    public String getResourceLocation() {
        return getRootDirectory().toUri().toString();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请先选择图片文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("仅支持 JPG、PNG、WEBP 图片");
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 JPG、PNG、WEBP 图片");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        if (extension == null) {
            throw new IllegalArgumentException("无法识别图片格式");
        }
        return extension;
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String buildImageUrl(String folder, String fileName) {
        return normalizeUrlPrefix() + "/" + folder + "/" + fileName;
    }

    private String normalizeUrlPrefix() {
        String trimmed = rewardUrlPrefix == null ? "" : rewardUrlPrefix.trim();
        if (trimmed.isEmpty()) {
            return "/uploads/rewards";
        }
        String normalized = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return normalized.replaceAll("/+$", "");
    }
}
