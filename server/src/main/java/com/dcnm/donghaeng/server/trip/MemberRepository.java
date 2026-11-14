package com.dcnm.donghaeng.server.trip;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberEntity, String> {

    /** 방에 남아 있는 차량: 입장 순서 */
    List<MemberEntity> findByTripIdAndLeftAtIsNullOrderByJoinedAtAsc(String tripId);

    Optional<MemberEntity> findByIdAndTripIdAndLeftAtIsNull(String id, String tripId);
}