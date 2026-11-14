package com.dcnm.donghaeng.server.photo;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * storage.type=s3. Supabase Storage 의 S3 호환 주소로 올린다.
 * forcePathStyle 및 체크섬 설정은 AWS 가 아닌 S3 호환 저장소에서 오류를 피하기 위함.
 */
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3PhotoStorage implements PhotoStorage {

    private final S3Client s3;
    private final String bucket;

    public S3PhotoStorage(StorageProperties props) {
        StorageProperties.S3 c = props.s3();
        this.bucket = c.bucket();
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(c.endpoint()))
                .region(Region.of(c.region()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(c.accessKey(), c.secretKey())))
                .forcePathStyle(true)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Override
    public void put(String key, byte[] bytes, String contentType) {
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(bytes));
    }

    @Override
    public byte[] get(String key) {
        return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}