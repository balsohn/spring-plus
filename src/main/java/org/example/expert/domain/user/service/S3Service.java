package org.example.expert.domain.user.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket-name:spring-plus-profile-images}")
    private String bucketName;

    public String uploadProfileImage(MultipartFile file, Long userId) {
        try {
            validateFile(file);

            String fileName = generateFileName(file, userId);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            amazonS3.putObject(bucketName, fileName, file.getInputStream(), metadata);

            URL url = amazonS3.getUrl(bucketName, fileName);
            log.info("프로필 이미지 업로드 성공: userId={}, fileName={}", userId, fileName);
            return url.toString();

        } catch (Exception e) {
            log.error("프로필 이미지 업로드 실패: userId={}", userId, e);
            throw new RuntimeException("프로필 이미지 업로드에 실패했습니다.", e);
        }
    }

    public void deleteProfileImage(String imageUrl) {
        try {
            String fileName = extractFileNameFromUrl(imageUrl);
            amazonS3.deleteObject(bucketName, fileName);
            log.info("프로필 이미지 삭제 성공: fileName={}", fileName);
        } catch (Exception e) {
            log.error("프로필 이미지 삭제 실패: fileName={}", imageUrl, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비었습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로그 가능합니다.");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 10MB 이하여야 합니다.");
        }
    }

    private String generateFileName(MultipartFile file, Long userId) {
        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return "profile-images/" + userId + "/" + UUID.randomUUID().toString() + extension;
    }

    private String extractFileNameFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.indexOf(bucketName) + bucketName.length() + 1);
    }
}
