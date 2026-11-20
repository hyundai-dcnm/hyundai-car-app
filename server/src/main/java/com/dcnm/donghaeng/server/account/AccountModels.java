package com.dcnm.donghaeng.server.account;

/**
 * contract/api.md "확장 API › 계정"·"모델 추가" 와 같은 JSON 형태.
 * 바꿀 때는 contract/api.md 를 먼저 수정.
 */
public final class AccountModels {
    private AccountModels() {}

    /** 내 계정. 친구 코드는 나에게만 보인다. */
    public record Account(String id, String nickname, String carLabel, String friendCode) {}

    /** 다른 사람에게 보이는 계정 정보(친구 목록·요청 등, 다음 단계에서 사용). */
    public record AccountSummary(String id, String nickname, String carLabel) {}

    /** POST /api/accounts 응답. accountToken 은 이때 한 번만 내려준다. */
    public record CreatedAccount(Account account, String accountToken) {}
}
