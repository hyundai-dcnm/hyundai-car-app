package com.dcnm.donghaeng.server.account;

import com.dcnm.donghaeng.server.account.AccountModels.Account;
import com.dcnm.donghaeng.server.account.AccountModels.AccountSummary;
import com.dcnm.donghaeng.server.account.AccountModels.CreatedAccount;
import com.dcnm.donghaeng.server.auth.AccountPrincipal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 기기 계정 만들기/조회/수정과 계정 토큰 확인.
 * 계정 토큰은 여행방 토큰과 같은 모양(43자)이지만 다른 표에 저장한다.
 */
@Service
public class AccountService {

    /** 초대 코드(TripService)와 같은 글자 규칙: 헷갈리는 0·O·1·I 제외. */
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SecureRandom random = new SecureRandom();
    private final AccountRepository accounts;
    private final AccountSessionRepository sessions;

    public AccountService(AccountRepository accounts, AccountSessionRepository sessions) {
        this.accounts = accounts;
        this.sessions = sessions;
    }

    // ───────── 만들기/조회/수정 ─────────

    @Transactional
    public CreatedAccount create(String nickname, String carLabel) {
        Instant now = Instant.now();
        AccountEntity account = accounts.save(
                new AccountEntity(newId(), nickname.trim(), carLabel.trim(), newFriendCode(), now));
        AccountSessionEntity session = sessions.save(
                new AccountSessionEntity(newId(), newToken(), account.getId(), now));
        return new CreatedAccount(toAccount(account), session.getToken());
    }

    @Transactional(readOnly = true)
    public Account me(AccountPrincipal me) {
        return toAccount(account(me.accountId()));
    }

    @Transactional
    public Account update(AccountPrincipal me, String nickname, String carLabel) {
        AccountEntity account = account(me.accountId());
        account.rename(nickname.trim(), carLabel.trim(), Instant.now());
        return toAccount(account);
    }

    // ───────── 토큰 ─────────

    /** 필터(AccessTokenFilter)가 부른다. 없는 토큰이면 비어 있음. */
    @Transactional(readOnly = true)
    public Optional<AccountPrincipal> resolve(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return sessions.findByTokenAndRevokedAtIsNull(token.trim())
                .map(s -> new AccountPrincipal(s.getId(), s.getAccountId()));
    }

    /**
     * 방 만들기·참여 본문의 accountToken(선택)을 계정 id 로 바꾼다.
     * 비어 있으면 null(계정 없이 참여), 틀린 토큰이면 401.
     */
    @Transactional(readOnly = true)
    public String accountIdForLink(String accountToken) {
        if (accountToken == null || accountToken.isBlank()) return null;
        return resolve(accountToken)
                .map(AccountPrincipal::accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "계정 토큰이 맞지 않아요"));
    }

    // ───────── 변환(다음 단계의 친구·주변 찾기에서도 사용) ─────────

    public static Account toAccount(AccountEntity a) {
        return new Account(a.getId(), a.getNickname(), a.getCarLabel(), a.getFriendCode());
    }

    public static AccountSummary toSummary(AccountEntity a) {
        return new AccountSummary(a.getId(), a.getNickname(), a.getCarLabel());
    }

    // ───────── 내부 헬퍼 ─────────

    private AccountEntity account(String accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "계정이 없어요"));
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String newFriendCode() {
        String code;
        do {
            var sb = new StringBuilder();
            for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            code = sb.toString();
        } while (accounts.existsByFriendCode(code));
        return code;
    }
}
