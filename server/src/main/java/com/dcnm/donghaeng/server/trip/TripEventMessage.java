package com.dcnm.donghaeng.server.trip;

/** 서비스 → WebSocket 으로 넘김: 어느 방에 보낼지(tripId)와 내용(event). */
public record TripEventMessage(String tripId, TripEvent event) {}