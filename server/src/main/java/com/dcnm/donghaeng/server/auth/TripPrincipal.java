package com.dcnm.donghaeng.server.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 토큰으로 알아낸 요청자 정보
 * CAR = 차량 앱(멤버), WEB = 웹 앨범 참여자(멤버 아님, memberId 없음).
 */
public record TripPrincipal(String sessionId, String tripId, String memberId, String displayName, Role role) {

    public enum Role { CAR, WEB }

    /** 주소의 {tripId} 와 토큰의 방이 같은지 확인. 다르면 403. */
    public TripPrincipal inTrip(String pathTripId) {
        if (!tripId.equals(pathTripId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 여행방의 토큰이 아니에요");
        }
        return this;
    }

    /** 사진 올린 사람 구분용 id. 차량은 memberId, 웹은 세션마다 다른 값. */
    public String subjectId() {
        return memberId != null ? memberId : "viewer-" + sessionId;
    }
}