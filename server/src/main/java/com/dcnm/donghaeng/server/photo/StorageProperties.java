package com.dcnm.donghaeng.server.photo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 사진 파일을 어디에 둘지.
 * - type=local : 내 컴퓨터 폴더(localDir). 개발용
 * - type=s3    : S3 호환 저장소(Supabase Storage). 배포용. 키 값은 환경 변수로만 삽입
 */
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(String type, String localDir, S3 s3) {

    public record S3(String endpoint, String region, String bucket, String accessKey, String secretKey) {}
}