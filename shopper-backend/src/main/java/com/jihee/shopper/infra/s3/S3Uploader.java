package com.jihee.shopper.infra.s3;

import com.jihee.shopper.global.exception.CustomException;
import com.jihee.shopper.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * S3 이미지 업로드 유틸리티 (ADR-03-010 ~ ADR-03-020).
 *
 * <p>상품 이미지를 S3에 업로드하고 URL을 반환한다.
 * <p>파일명: UUID + 확장자 (ADR-03-010)
 * <p>경로: products/YYYY/MM/DD/ (ADR-03-011)
 * <p>제한: 5MB, jpg/jpeg/png/webp (ADR-03-015, ADR-03-016)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;  // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    // ── 이미지 업로드 ──────────────────────────────────────────────────────────

    /**
     * 이미지 파일을 S3에 업로드하고 URL을 반환한다.
     *
     * @param file 업로드할 이미지 파일
     * @return S3 URL (https://{bucket}.s3.{region}.amazonaws.com/{key})
     */
    public String uploadImage(MultipartFile file) {
        validateFile(file);

        String fileName = generateFileName(file.getOriginalFilename());
        String s3Key = generateS3Key(fileName);

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

            String url = generateS3Url(s3Key);
            log.info("S3 업로드 성공: {}", url);
            return url;

        } catch (S3Exception e) {
            log.error("S3 업로드 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (IOException e) {
            log.error("파일 읽기 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // ── 이미지 삭제 ────────────────────────────────────────────────────────────

    /**
     * S3 URL에서 이미지를 삭제한다 (ADR-03-006).
     *
     * @param imageUrl S3 URL
     */
    public void deleteImage(String imageUrl) {
        String s3Key = extractS3Key(imageUrl);

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("S3 삭제 성공: {}", s3Key);

        } catch (S3Exception e) {
            log.error("S3 삭제 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // ── 파일 검증 ──────────────────────────────────────────────────────────────

    /**
     * 파일 크기, 타입, 확장자 검증 (ADR-03-015, ADR-03-016).
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 파일 크기 검증 (5MB)
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }

        // 확장자 검증
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    // ── 파일명 생성 ────────────────────────────────────────────────────────────

    /**
     * UUID 기반 파일명 생성 (ADR-03-010).
     *
     * @param originalFilename 원본 파일명
     * @return UUID + 확장자 (예: "a3f2c1b5-8d4e-4a2f-9e1b-3c5d7e9f1a2b.jpg")
     */
    private String generateFileName(String originalFilename) {
        String extension = getExtension(originalFilename);
        return UUID.randomUUID() + "." + extension;
    }

    /**
     * 날짜별 S3 키 생성 (ADR-03-011).
     *
     * @param fileName UUID 파일명
     * @return S3 키 (예: "products/2026/02/20/a3f2c1b5-8d4e-4a2f-9e1b-3c5d7e9f1a2b.jpg")
     */
    private String generateS3Key(String fileName) {
        LocalDate now = LocalDate.now();
        return String.format("products/%d/%02d/%02d/%s",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(), fileName);
    }

    /**
     * S3 URL 생성.
     *
     * @param s3Key S3 키
     * @return S3 URL (https://{bucket}.s3.{region}.amazonaws.com/{key})
     */
    private String generateS3Url(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, s3Key);
    }

    /**
     * S3 URL에서 키 추출.
     *
     * @param imageUrl S3 URL
     * @return S3 키 (예: "products/2026/02/20/xxx.jpg")
     */
    private String extractS3Key(String imageUrl) {
        // https://{bucket}.s3.{region}.amazonaws.com/{key} 형식에서 key 추출
        String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucket, region);
        if (imageUrl.startsWith(prefix)) {
            return imageUrl.substring(prefix.length());
        }
        throw new CustomException(ErrorCode.INVALID_INPUT);
    }

    /**
     * 파일 확장자 추출.
     *
     * @param filename 파일명
     * @return 확장자 (소문자)
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
