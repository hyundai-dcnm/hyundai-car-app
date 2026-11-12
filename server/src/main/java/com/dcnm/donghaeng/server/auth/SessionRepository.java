package com.dcnm.donghaeng.server.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionEntity, String> {

    Optional<SessionEntity> findByTokenAndRevokedAtIsNull(String token);

    List<SessionEntity> findByTripIdAndMemberIdAndRevokedAtIsNull(String tripId, String memberId);

    List<SessionEntity> findByTripIdAndRevokedAtIsNull(String tripId);
}