package com.dcnm.donghaeng.server.photo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<PhotoEntity, String> {

    List<PhotoEntity> findByTripIdOrderByCreatedAtDesc(String tripId);

    Optional<PhotoEntity> findByIdAndTripId(String id, String tripId);
}