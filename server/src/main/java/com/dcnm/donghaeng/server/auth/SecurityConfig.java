package com.dcnm.donghaeng.server.auth;

import com.dcnm.donghaeng.server.account.AccountService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 누가 어떤 주소를 부를 수 있는지. 위에서부터 차례로 맞춰 본다.
 * - 토큰 없이: 방 생성 및 참여(차량/웹), 계정 만들기, WebSocket 입구(토큰은 핸들러가 직접 확인), 상태 확인(health)
 * - 계정 토큰만: /api/me, /api/friends, /api/blocks, /api/nearby
 * - 여행방 토큰만: /api/trips/**
 * 토큰이 없거나 틀리면 401, 다른 종류의 토큰이면 403.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccessTokenService tokens, AccountService accounts,
                                            @Value("${app.cors.allowed-origins}") String[] allowedOrigins) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .cors(cors -> cors.configurationSource(corsSource(allowedOrigins)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 토큰 없이 허용하는 줄이 역할 검사 줄보다 위에 있어야 방 만들기·참여가 기존처럼 토큰 없이 된다.
                        .requestMatchers(HttpMethod.POST, "/api/trips", "/api/trips/join", "/api/trips/web-join",
                                "/api/accounts").permitAll()
                        .requestMatchers("/ws/**", "/actuator/health", "/actuator/health/**", "/error").permitAll()
                        .requestMatchers("/api/me", "/api/me/**", "/api/friends/**", "/api/blocks/**", "/api/nearby/**")
                                .hasRole("ACCOUNT")
                        .requestMatchers("/api/trips/**").hasAnyRole("CAR", "WEB")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new AccessTokenFilter(tokens, accounts), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 웹이 다른 주소(예: 웹 배포 주소, 개발 중 localhost:5173)에서 /api 를 부를 수 있게 허용. */
    private static CorsConfigurationSource corsSource(String[] allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}