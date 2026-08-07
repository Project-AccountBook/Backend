package com.chaewookim.accountbookformoms.global.common.file;

import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileUploadService {

    static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Map<String, Set<String>> ALLOWED_EXTENSION_TO_CONTENT_TYPES = Map.of(
            ".jpg", Set.of("image/jpeg", "image/jpg"),
            ".jpeg", Set.of("image/jpeg", "image/jpg"),
            ".png", Set.of("image/png"),
            ".webp", Set.of("image/webp"),
            ".gif", Set.of("image/gif")
    );

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    public String uploadFile(MultipartFile file) {
        validate(file);

        String extension = resolveExtension(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType());
        String uniqueFileName = UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));

            // 업로드된 파일의 전체를 브라우저에서 바로 볼 수 있는 S3 주소
            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + uniqueFileName;
        } catch (Exception e) {
            log.error("AWS S3 파일 업로드 실패", e);
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.");
        }
    }

    void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }

        String extension = resolveExtension(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType());
        Set<String> allowedContentTypes = ALLOWED_EXTENSION_TO_CONTENT_TYPES.get(extension);

        if (allowedContentTypes == null || contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private static String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT).trim();
        int semicolon = normalized.indexOf(';');
        if (semicolon >= 0) {
            normalized = normalized.substring(0, semicolon).trim();
        }
        return normalized;
    }
}
