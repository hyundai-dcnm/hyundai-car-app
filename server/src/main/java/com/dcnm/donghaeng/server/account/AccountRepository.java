package com.dcnm.donghaeng.server.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {

    /** 친구 요청(다음 단계)에서 친구 코드로 상대를 찾을 때 쓴다. */
    Optional<AccountEntity> findByFriendCode(String friendCode);

    boolean existsByFriendCode(String friendCode);
}
