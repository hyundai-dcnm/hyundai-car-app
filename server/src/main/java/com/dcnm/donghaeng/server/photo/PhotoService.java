package com.dcnm.donghaeng.server.photo;

import com.dcnm.donghaeng.server.auth.TripPrincipal;
import com.dcnm.donghaeng.server.trip.TripEvent;
import com.dcnm.donghaeng.server.trip.TripEventMessage;
import com.dcnm.donghaeng.server.trip.TripModels.Photo;
import com.dcnm.donghaeng.server.trip.TripService;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** 공동 앨범. 같은 방 토큰을 가진 차량과 웹만 올리고 볼 수 있다. */
@Service
public class PhotoService {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final PhotoRepository photos;
    private final PhotoStorage storage;
    private final TripService trips;
    private final ApplicationEventPublisher events;

    public PhotoService(PhotoRepository photos, PhotoStorage storage, TripService trips,
                        ApplicationEventPublisher events) {
        this.photos = photos;
        this.storage = storage;
        this.trips = trips;
        this.events = events;
    }

    public record PhotoFile(byte[] bytes, String contentType) {}

    @Transactional
    public Photo upload(TripPrincipal me, MultipartFile file) {
        trips.trip(me); // 닫힌 방이면 여기서 404
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진 파일이 없어요");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진(image/*)만 올릴 수 있어요");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진은 10MB 까지 올릴 수 있어요");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진을 읽지 못했어요");
        }
        String id = UUID.randomUUID().toString();
        String key = me.tripId() + "/" + id + extension(contentType);
        storage.put(key, bytes, contentType);
        PhotoEntity saved = photos.save(new PhotoEntity(id, me.tripId(), me.subjectId(), me.displayName(), key,
                contentType, bytes.length, Instant.now()));
        Photo dto = toPhoto(saved);
        events.publishEvent(new TripEventMessage(me.tripId(), new TripEvent.PhotoAdded(dto)));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<Photo> list(TripPrincipal me) {
        return photos.findByTripIdOrderByCreatedAtDesc(me.tripId()).stream().map(PhotoService::toPhoto).toList();
    }

    @Transactional(readOnly = true)
    public PhotoFile file(TripPrincipal me, String photoId) {
        PhotoEntity photo = find(me, photoId);
        return new PhotoFile(storage.get(photo.getObjectKey()), photo.getContentType());
    }

    /** 올린 사람 또는 방장만 지울 수 있다. */
    @Transactional
    public void delete(TripPrincipal me, String photoId) {
        PhotoEntity photo = find(me, photoId);
        boolean mine = photo.getUploaderId().equals(me.subjectId());
        if (!mine && !trips.isHost(me.tripId(), me.memberId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "올린 사람이나 방장만 지울 수 있어요");
        }
        storage.delete(photo.getObjectKey());
        photos.delete(photo);
        events.publishEvent(new TripEventMessage(me.tripId(), new TripEvent.PhotoDeleted(photoId)));
    }

    private PhotoEntity find(TripPrincipal me, String photoId) {
        return photos.findByIdAndTripId(photoId, me.tripId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사진이 없어요"));
    }

    private static Photo toPhoto(PhotoEntity p) {
        String url = "/api/trips/" + p.getTripId() + "/photos/" + p.getId() + "/file";
        return new Photo(p.getId(), p.getUploaderName(), p.getContentType(), p.getSizeBytes(),
                p.getCreatedAt().toEpochMilli(), url);
    }

    private static String extension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/heic" -> ".heic";
            default -> "";
        };
    }
}