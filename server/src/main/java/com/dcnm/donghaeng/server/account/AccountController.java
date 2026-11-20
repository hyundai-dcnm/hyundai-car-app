package com.dcnm.donghaeng.server.account;

import com.dcnm.donghaeng.server.account.AccountModels.Account;
import com.dcnm.donghaeng.server.account.AccountModels.CreatedAccount;
import com.dcnm.donghaeng.server.auth.AccountPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** contract/api.md "확장 API › 계정". */
@RestController
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    public record AccountRequest(
            @NotBlank @Size(max = 20) String nickname,
            @NotBlank @Size(max = 20) String carLabel) {}

    /** 앱 첫 실행 때 한 번. 토큰 없이 부른다(SecurityConfig 에서 허용). */
    @PostMapping("/api/accounts")
    public CreatedAccount create(@Valid @RequestBody AccountRequest body) {
        return accounts.create(body.nickname(), body.carLabel());
    }

    @GetMapping("/api/me")
    public Account me(@AuthenticationPrincipal AccountPrincipal me) {
        return accounts.me(me);
    }

    @PutMapping("/api/me")
    public Account update(@AuthenticationPrincipal AccountPrincipal me, @Valid @RequestBody AccountRequest body) {
        return accounts.update(me, body.nickname(), body.carLabel());
    }
}
