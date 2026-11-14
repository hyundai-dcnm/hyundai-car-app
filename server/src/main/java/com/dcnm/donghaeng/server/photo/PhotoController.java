package com.dcnm.donghaeng.server.photo;

import com.dcnm.donghaeng.server.auth.TripPrincipal;
import com.dcnm.donghaeng.server.trip.TripModels.Photo;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/trips/{tripId}/photos")
public class PhotoController {

    private final PhotoService photos;

    public PhotoController(PhotoService photos) {
        this.photos = photos;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Photo upload(@PathVariable String tripId, @AuthenticationPrincipal TripPrincipal me,
                        @RequestParam("file") MultipartFile file) {
        return photos.upload(me.inTrip(tripId), file);
    }

    @GetMapping
    public List<Photo> list(@PathVariable String tripId, @AuthenticationPrincipal TripPrincipal me) {
        return photos.list(me.inTrip(tripId));
    }

    /** 웹 <img src="...?token=..."> 으로도 부를 수 있다(GET 만 ?token 허용). */
    @GetMapping("/{photoId}/file")
    public ResponseEntity<byte[]> file(@PathVariable String tripId, @PathVariable String photoId,
                                       @AuthenticationPrincipal TripPrincipal me) {
        PhotoService.PhotoFile f = photos.file(me.inTrip(tripId), photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(f.bytes());
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String tripId, @PathVariable String photoId,
                       @AuthenticationPrincipal TripPrincipal me) {
        photos.delete(me.inTrip(tripId), photoId);
    }
}