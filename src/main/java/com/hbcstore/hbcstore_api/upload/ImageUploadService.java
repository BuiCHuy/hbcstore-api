package com.hbcstore.hbcstore_api.upload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class ImageUploadService {
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final String bucket;
    private final String publicBaseUrl;
    private final String keyPrefix;
    private final S3Client s3Client;

    public ImageUploadService(
            @Value("${S3_BUCKET:}") String bucket,
            @Value("${AWS_REGION:ap-southeast-1}") String region,
            @Value("${S3_PUBLIC_BASE_URL:}") String publicBaseUrl,
            @Value("${app.upload.s3-prefix:uploads}") String keyPrefix
    ) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3_BUCKET is required");
        }
        this.bucket = bucket.trim();
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        this.keyPrefix = keyPrefix == null ? "uploads" : keyPrefix.trim();
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    public List<String> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Chưa có tệp nào được chọn");
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            validateFile(file);
            String key = buildObjectKey(file);
            putObject(file, key);
            urls.add(buildPublicUrl(key));
        }

        if (urls.isEmpty()) {
            throw new IllegalArgumentException("Không có tệp ảnh hợp lệ nào được chọn");
        }

        return urls;
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ được phép tải lên tệp ảnh");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Ảnh vượt quá giới hạn 5MB");
        }
    }

    private String buildObjectKey(MultipartFile file) {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = "";
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot >= 0) {
            extension = originalName.substring(lastDot).toLowerCase();
        }
        String safePrefix = keyPrefix.endsWith("/") ? keyPrefix.substring(0, keyPrefix.length() - 1) : keyPrefix;
        return safePrefix + "/" + UUID.randomUUID() + extension;
    }

    private void putObject(MultipartFile file, String key) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc tệp ảnh", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tải ảnh lên hệ thống lưu trữ", exception);
        }
    }

    private String buildPublicUrl(String key) {
        if (!publicBaseUrl.isBlank()) {
            String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
            return base + "/" + key;
        }
        return "https://" + bucket + ".s3.amazonaws.com/" + key;
    }
}
