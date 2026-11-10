package com.dcnm.donghaeng.server.call;

import com.dcnm.donghaeng.server.trip.TripModels.CallCredentials;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 여행방 하나 = LiveKit 방 하나. identity 는 멤버 id 라서 앱이 통화 참가자와 일행을 맞출 수 있다. */
@Service
public class CallTokenService {

	private final LiveKitProperties props;

	public CallTokenService(LiveKitProperties props) {
		this.props = props;
	}

	public CallCredentials issue(String tripId, String memberId, String displayName) {
		if (!props.configured()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "LiveKit 설정이 없어요 (LIVEKIT_URL 등)");
		}
		AccessToken token = new AccessToken(props.apiKey(), props.apiSecret());
		token.setIdentity(memberId);
		token.setName(displayName);
		token.addGrants(new RoomJoin(true), new RoomName("trip-" + tripId));
		return new CallCredentials(props.url(), token.toJwt());
	}
}
