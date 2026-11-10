# 앱·서버·웹 연동 규약

앱(`core/model`, `core/network`), 서버(`server/.../trip`), 웹(`web/src/types.ts`)이 같은 모양을 쓴다.
바꿀 때는 이 문서를 먼저 고치고 세 곳을 같이 바꾼다.

## 인증

방을 만들거나 참여하면 `accessToken`을 받는다. 이후 REST는 `Authorization: Bearer <accessToken>`,
WebSocket은 `?token=<accessToken>`으로 보낸다. 토큰 하나는 방 하나에만 유효하다.
계정·로그인은 아직 없다 (④ 후속).

## REST

| 메서드 | 경로 | 요청 | 응답 |
|---|---|---|---|
| POST | `/api/trips` | `{name, nickname, carLabel}` | `JoinedTrip` |
| POST | `/api/trips/join` | `{inviteCode, nickname, carLabel}` | `JoinedTrip` (없는 코드면 404) |
| DELETE | `/api/trips/{tripId}/members/me` | | 204 |
| PUT | `/api/trips/{tripId}/members/me/sharing` | `{sharing}` | 204 |
| POST | `/api/trips/{tripId}/proposals` | `{destination}` | `DestinationProposal` |
| POST | `/api/trips/{tripId}/proposals/{proposalId}/accept` | | 204 (지난 제안이면 409) |
| POST | `/api/trips/{tripId}/call-token` | | `{url, token}` (LiveKit 미설정이면 503) |

`JoinedTrip = {trip: Trip, memberId, accessToken}`

## WebSocket `/ws/trips/{tripId}?token=...`

접속하면 서버가 `snapshot`을 먼저 보낸다. 재접속해도 똑같이 보내므로 앱은 끊긴 동안의 이벤트를 따로 맞추지 않는다.

모든 메시지는 `type` 필드로 구분한다.

| type | 방향 | 내용 |
|---|---|---|
| `snapshot` | 서버→앱 | `{trip, locations: MemberLocation[]}` |
| `member_joined` | 서버→앱 | `{member}` |
| `member_left` | 서버→앱 | `{memberId}` |
| `location` | 양방향 | `{location: MemberLocation}`. 앱이 보내면 서버가 memberId·시각을 덮어써서 중계 |
| `location_sharing` | 서버→앱 | `{memberId, sharing}`. false면 그 차의 마지막 위치를 지운다 |
| `destination_proposed` | 서버→앱 | `{proposal}` |
| `destination_accepted` | 서버→앱 | `{proposalId, memberId}` |
| `destination_confirmed` | 서버→앱 | `{destination}`. 모든 차가 수락하면. 각 앱이 자기 내비에 안내 요청 |

## 모델

```
LatLng              {lat, lng}
Destination         {name, location: LatLng}
Member              {id, nickname, carLabel, sharingLocation}
Trip                {id, name, inviteCode, hostId, members: Member[], destination?, proposal?}
DestinationProposal {id, proposerId, destination, acceptedBy: string[]}
MemberLocation      {memberId, location: LatLng, headingDeg?, speedKph?, updatedAtMillis}
```

## 통화

여행방 하나 = LiveKit 방 `trip-{tripId}` 하나. 토큰의 identity는 memberId, name은 carLabel.
앱은 identity로 통화 참가자와 일행 목록을 맞춘다.
