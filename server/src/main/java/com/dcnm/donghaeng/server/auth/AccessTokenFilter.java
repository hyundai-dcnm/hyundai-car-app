package com.dcnm.donghaeng.server.auth;

import com.dcnm.donghaeng.server.account.AccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 요청에서 토큰을 꺼내 확인한다.
 * - 보통: 헤더 Authorization: Bearer <토큰>
 * - GET 요청만: 주소 뒤 ?token=<토큰> (웹 <img> 태그·WebSocket 처럼 헤더를 못 붙이는 경우)
 * 여행방 토큰이면 ROLE_CAR/ROLE_WEB, 계정 토큰이면 ROLE_ACCOUNT. 어느 주소에 어느 역할이 필요한지는 SecurityConfig 가 정한다.
 * 일부러 @Component 를 붙이지 않는다(붙이면 필터가 두 번 등록됨). SecurityConfig 에서 직접 만든다.
 */
public class AccessTokenFilter extends OncePerRequestFilter {

    private final AccessTokenService tokens;
    private final AccountService accounts;

    public AccessTokenFilter(AccessTokenService tokens, AccountService accounts) {
        this.tokens = tokens;
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extract(request);
        if (token != null) {
            authenticate(token).ifPresent(authentication -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            });
        }
        chain.doFilter(request, response);
    }

    /** 여행방 토큰을 먼저 찾고, 없으면 계정 토큰을 찾는다. */
    private Optional<Authentication> authenticate(String token) {
        Optional<Authentication> trip = tokens.resolve(token)
                .map(principal -> authentication(principal, "ROLE_" + principal.role().name()));
        if (trip.isPresent()) return trip;
        return accounts.resolve(token).map(principal -> authentication(principal, "ROLE_ACCOUNT"));
    }

    private static Authentication authentication(Object principal, String role) {
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority(role)));
    }

    private static String extract(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        if ("GET".equals(request.getMethod())) {
            String query = request.getParameter("token");
            if (query != null && !query.isBlank()) return query.trim();
        }
        return null;
    }
}
