package com.dcnm.donghaeng.server.trip;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<TripEntity, String> {

    Optional<TripEntity> findByInviteCodeAndClosedAtIsNull(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    /** 같은 방을 동시에 바꾸는 요청(동시 수락 등)이 덮어쓰이지 않도록 Lock을 사용한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TripEntity t where t.id = :id")
    Optional<TripEntity> findByIdForUpdate(@Param("id") String id);
}