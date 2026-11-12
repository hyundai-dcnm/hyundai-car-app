package com.dcnm.donghaeng.server.trip;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<ProposalEntity, String> {

    /** 수락을 기다리는 제안은 방마다 하나 */
    Optional<ProposalEntity> findFirstByTripIdAndStatus(String tripId, ProposalEntity.Status status);
}