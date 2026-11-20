package com.dcnm.donghaeng.server.auth;

/**
 * 계정 토큰으로 알아낸 요청자 정보.
 * 여행방 토큰의 TripPrincipal 과 다르며, /api/me·/api/friends·/api/blocks·/api/nearby 에서만 쓰인다.
 */
public record AccountPrincipal(String sessionId, String accountId) {}
