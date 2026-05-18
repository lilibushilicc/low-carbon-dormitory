package com.example.lowcarbondormitory.service.admin;

import com.example.lowcarbondormitory.dto.response.AdminRewardImageUploadResponse;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class AdminRewardImageStorageService {

    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final String endpoint;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String region;
    private final String bucketName;
    private final String publicBaseUrl;
    private final AdminR2StorageConfigService r2StorageConfigService;

    public AdminRewardImageStorageService(
            AdminR2StorageConfigService r2StorageConfigService,
            @Value("${app.upload.r2.endpoint:}") String endpoint,
            @Value("${app.upload.r2.access-key-id:}") String accessKeyId,
            @Value("${app.upload.r2.secret-access-key:}") String secretAccessKey,
            @Value("${app.upload.r2.region:auto}") String region,
            @Value("${app.upload.r2.bucket:}") String bucketName,
            @Value("${app.upload.r2.public-base-url:}") String publicBaseUrl
    ) {
        this.r2StorageConfigService = r2StorageConfigService;
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.region = region;
        this.bucketName = bucketName;
        this.publicBaseUrl = publicBaseUrl;
    }

    public AdminRewardImageUploadResponse store(MultipartFile file) {
        validateFile(file);

        String extension = resolveExtension(file);
        String folder = LocalDate.now().format(MONTH_FORMATTER);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String objectKey = folder + "/" + fileName;
        R2UploadConfig uploadConfig = resolveUploadConfig();

        try (S3Client s3Client = createS3Client(uploadConfig)) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(uploadConfig.bucket())
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception ex) {
            throw new IllegalStateException("图片上传失败", ex);
        }

        AdminRewardImageUploadResponse response = new AdminRewardImageUploadResponse();
        response.setImageUrl(buildImageUrl(uploadConfig.publicBaseUrl(), objectKey));
        response.setOriginalName(file.getOriginalFilename());
        response.setSize(file.getSize());
        return response;
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

    private String buildImageUrl(String publicBaseUrl, String objectKey) {
        return normalizePublicBaseUrl(publicBaseUrl) + "/" + objectKey;
    }

    private S3Client createS3Client(R2UploadConfig config) {
        if (isBlank(config.endpoint()) || isBlank(config.accessKeyId()) || isBlank(config.secretAccessKey()) || isBlank(config.bucket())) {
            throw new IllegalStateException("未配置完整的 R2 连接信息");
        }

        return S3Client.builder()
                .endpointOverride(URI.create(config.endpoint()))
                .region(Region.of(config.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKeyId(), config.secretAccessKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private R2UploadConfig resolveUploadConfig() {
        var response = r2StorageConfigService.getCurrentConfig();
        if (response.isConfigured()) {
            return new R2UploadConfig(
                    response.getEndpoint(),
                    response.getAccessKeyId(),
                    response.getSecretAccessKey(),
                    response.getBucket(),
                    response.getPublicBaseUrl(),
                    response.getRegion()
            );
        }
        return new R2UploadConfig(endpoint, accessKeyId, secretAccessKey, bucketName, publicBaseUrl, region);
    }

    private String normalizePublicBaseUrl(String publicBaseUrl) {
        String trimmed = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException("未配置 R2 公网访问地址");
        }
        return trimmed.replaceAll("/+$", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record R2UploadConfig(
            String endpoint,
            String accessKeyId,
            String secretAccessKey,
            String bucket,
            String publicBaseUrl,
            String region
    ) {
    }
}
