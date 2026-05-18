package com.example.lowcarbondormitory.service.admin;

import com.example.lowcarbondormitory.dto.request.AdminR2StorageConfigRequest;
import com.example.lowcarbondormitory.dto.response.AdminR2StorageConfigResponse;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Service
public class AdminR2StorageConfigService {

    private static final String CONFIG_KEY = "reward_image_r2";

    private final JdbcTemplate jdbcTemplate;
    private final String defaultEndpoint;
    private final String defaultAccessKeyId;
    private final String defaultSecretAccessKey;
    private final String defaultBucket;
    private final String defaultPublicBaseUrl;
    private final String defaultRegion;

    public AdminR2StorageConfigService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.upload.r2.endpoint:}") String defaultEndpoint,
            @Value("${app.upload.r2.access-key-id:}") String defaultAccessKeyId,
            @Value("${app.upload.r2.secret-access-key:}") String defaultSecretAccessKey,
            @Value("${app.upload.r2.bucket:}") String defaultBucket,
            @Value("${app.upload.r2.public-base-url:}") String defaultPublicBaseUrl,
            @Value("${app.upload.r2.region:auto}") String defaultRegion
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.defaultEndpoint = defaultEndpoint;
        this.defaultAccessKeyId = defaultAccessKeyId;
        this.defaultSecretAccessKey = defaultSecretAccessKey;
        this.defaultBucket = defaultBucket;
        this.defaultPublicBaseUrl = defaultPublicBaseUrl;
        this.defaultRegion = defaultRegion;
    }

    public AdminR2StorageConfigResponse getCurrentConfig() {
        ensureTable();

        List<AdminR2StorageConfigResponse> rows;
        try {
            rows = jdbcTemplate.query(
                    "SELECT endpoint, access_key_id, secret_access_key, bucket, public_base_url, region "
                            + "FROM system_storage_r2_config WHERE config_key = ?",
                    (rs, rowNum) -> {
                        AdminR2StorageConfigResponse response = new AdminR2StorageConfigResponse();
                        response.setEndpoint(rs.getString("endpoint"));
                        response.setAccessKeyId(rs.getString("access_key_id"));
                        response.setSecretAccessKey(rs.getString("secret_access_key"));
                        response.setBucket(rs.getString("bucket"));
                        response.setPublicBaseUrl(rs.getString("public_base_url"));
                        response.setRegion(rs.getString("region"));
                        return response;
                    },
                    CONFIG_KEY
            );
        } catch (DataAccessException exception) {
            throw buildPersistenceException("读取", exception);
        }

        AdminR2StorageConfigResponse response = rows.isEmpty() ? buildDefaultConfig() : rows.get(0);
        normalizeResponse(response);
        return response;
    }

    @Transactional
    public AdminR2StorageConfigResponse saveConfig(AdminR2StorageConfigRequest request) {
        ensureTable();
        String endpoint = trimRequired(request.getEndpoint(), "R2 endpoint 不能为空");
        String accessKeyId = trimRequired(request.getAccessKeyId(), "Access Key ID 不能为空");
        String secretAccessKey = trimRequired(request.getSecretAccessKey(), "Secret Access Key 不能为空");
        String bucket = trimRequired(request.getBucket(), "Bucket 不能为空");
        String publicBaseUrl = trimRequired(request.getPublicBaseUrl(), "公网访问地址不能为空");
        String region = trimToDefault(request.getRegion(), "auto");

        try {
            int updated = jdbcTemplate.update(
                    "UPDATE system_storage_r2_config "
                            + "SET endpoint = ?, access_key_id = ?, secret_access_key = ?, bucket = ?, public_base_url = ?, region = ?, update_time = ? "
                            + "WHERE config_key = ?",
                    endpoint,
                    accessKeyId,
                    secretAccessKey,
                    bucket,
                    publicBaseUrl,
                    region,
                    LocalDateTime.now(),
                    CONFIG_KEY
            );

            if (updated == 0) {
                jdbcTemplate.update(
                        "INSERT INTO system_storage_r2_config "
                                + "(config_key, endpoint, access_key_id, secret_access_key, bucket, public_base_url, region, update_time) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        CONFIG_KEY,
                        endpoint,
                        accessKeyId,
                        secretAccessKey,
                        bucket,
                        publicBaseUrl,
                        region,
                        LocalDateTime.now()
                );
            }
        } catch (DataAccessException exception) {
            throw buildPersistenceException("保存", exception);
        }

        return getCurrentConfig();
    }

    public boolean testConfig(AdminR2StorageConfigRequest request) {
        String endpoint = trimRequired(request.getEndpoint(), "R2 endpoint 不能为空");
        String accessKeyId = trimRequired(request.getAccessKeyId(), "Access Key ID 不能为空");
        String secretAccessKey = trimRequired(request.getSecretAccessKey(), "Secret Access Key 不能为空");
        String bucket = trimRequired(request.getBucket(), "Bucket 不能为空");
        String region = trimToDefault(request.getRegion(), "auto");

        try (S3Client client = createClient(endpoint, accessKeyId, secretAccessKey, region)) {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (Exception exception) {
            throw new IllegalStateException("R2 连接测试失败: " + exception.getMessage(), exception);
        }
    }

    private AdminR2StorageConfigResponse buildDefaultConfig() {
        AdminR2StorageConfigResponse response = new AdminR2StorageConfigResponse();
        response.setEndpoint(defaultEndpoint);
        response.setAccessKeyId(defaultAccessKeyId);
        response.setSecretAccessKey(defaultSecretAccessKey);
        response.setBucket(defaultBucket);
        response.setPublicBaseUrl(defaultPublicBaseUrl);
        response.setRegion(defaultRegion);
        return response;
    }

    private void normalizeResponse(AdminR2StorageConfigResponse response) {
        response.setEndpoint(trimToNull(response.getEndpoint()));
        response.setAccessKeyId(trimToNull(response.getAccessKeyId()));
        response.setSecretAccessKey(trimToNull(response.getSecretAccessKey()));
        response.setBucket(trimToNull(response.getBucket()));
        response.setPublicBaseUrl(trimToNull(response.getPublicBaseUrl()));
        response.setRegion(trimToDefault(response.getRegion(), "auto"));
        response.setConfigured(
                response.getEndpoint() != null
                        && response.getAccessKeyId() != null
                        && response.getSecretAccessKey() != null
                        && response.getBucket() != null
                        && response.getPublicBaseUrl() != null
        );
    }

    private S3Client createClient(String endpoint, String accessKeyId, String secretAccessKey, String region) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private void ensureTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS system_storage_r2_config ("
                            + "config_key varchar(64) PRIMARY KEY,"
                            + "endpoint varchar(255) NOT NULL,"
                            + "access_key_id varchar(255) NOT NULL,"
                            + "secret_access_key varchar(255) NOT NULL,"
                            + "bucket varchar(255) NOT NULL,"
                            + "public_base_url varchar(255) NOT NULL,"
                            + "region varchar(50) NOT NULL,"
                            + "update_time timestamp without time zone NOT NULL DEFAULT now()"
                            + ")"
            );
        } catch (DataAccessException exception) {
            throw buildPersistenceException("初始化", exception);
        }
    }

    private IllegalStateException buildPersistenceException(String action, DataAccessException exception) {
        Throwable rootCause = exception.getMostSpecificCause();
        String detail = rootCause != null && rootCause.getMessage() != null && !rootCause.getMessage().isBlank()
                ? rootCause.getMessage()
                : exception.getMessage();
        return new IllegalStateException("R2 配置" + action + "失败，请检查数据库结构或连接: " + detail, exception);
    }

    private String trimRequired(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
