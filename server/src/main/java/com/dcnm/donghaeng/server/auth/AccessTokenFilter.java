package com.dcnm.donghaeng.server.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 요청에서 토큰을 꺼내 확인한다.
 * - 보통: 헤더 Authorization: Bearer <토큰>
 * - GET 요청만: 주소 뒤 ?token=<토큰> (웹 <img> 태그·WebSocket 처럼 헤더를 못 붙이는 경우)
 * 일부러 @Component 를 붙이지 않는다(붙이면 필터가 두 번 등록됨). SecurityConfig 에서 직접 만든다.
 */
public class AccessTokenFilter extends OncePerRequestFilter {

    private final AccessTokenService tokens;

    public AccessTokenFilter(AccessTokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extract(request);
        if (token != null) {
            tokens.resolve(token).ifPresent(principal -> {
                var authentication = new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            });
        }
        chain.doFilter(request, response);
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