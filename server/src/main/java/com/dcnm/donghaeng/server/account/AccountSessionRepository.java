package com.dcnm.donghaeng.server.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountSessionRepository extends JpaRepository<AccountSessionEntity, String> {

    Optional<AccountSessionEntity> findByTokenAndRevokedAtIsNull(String token);
}
