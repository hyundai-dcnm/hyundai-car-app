package com.dcnm.donghaeng.server.photo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** DB 표 trip_photos: 사진 정보. 파일 자체는 PhotoStorage 에 있고 여기에는 위치(object_key)만 둔다. */
@Entity
@Table(name = "trip_photos", indexes = @Index(name = "idx_photos_trip", columnList = "trip_id"))
public class PhotoEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    /** 차량이면 멤버 id, 웹이면 "viewer-세션id". 삭제 권한 확인용. */
    @Column(name = "uploader_id", nullable = false, length = 50)
    private String uploaderId;

    @Column(name = "uploader_name", nullable = false, length = 20)
    private String uploaderName;

    @Column(name = "object_key", nullable = false, length = 200)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PhotoEntity() {}

    public PhotoEntity(String id, String tripId, String uploaderId, String uploaderName, String objectKey,
                       String contentType, long sizeBytes, Instant createdAt) {
        this.id = id;
        this.tripId = tripId;
        this.uploaderId = uploaderId;
        this.uploaderName = uploaderName;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTripId() { return tripId; }
    public String getUploaderId() { return uploaderId; }
    public String getUploaderName() { return uploaderName; }
    public String getObjectKey() { return objectKey; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getCreatedAt() { return createdAt; }
}