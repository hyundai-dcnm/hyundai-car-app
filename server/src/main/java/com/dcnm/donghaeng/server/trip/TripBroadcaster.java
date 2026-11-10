package com.dcnm.donghaeng.server.trip;

/** 여행방 접속자 전원에게 이벤트를 보낸다. 구현은 ws/TripSocketHandler. */
public interface TripBroadcaster {
	void send(String tripId, TripEvent event);
}
