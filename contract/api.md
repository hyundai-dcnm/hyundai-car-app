# 앱·서버·웹 연동 규약

차량 앱(`core/model`, `core/trip`), 서버(`server/`), 웹(`web/src/types.ts`)이 같은 모양을 사용한다.
바꿀 때는 이 문서를 고치는 PR 을 먼저 올리고, 다른 세 역할 확인 후 같이 바꾼다.

## 서버 주소

| 환경 | REST | WebSocket |
|---|---|---|
| 내 컴퓨터 | `http://localhost:8080` | `ws://localhost:8080` |
| 에뮬레이터 → 내 컴퓨터 | `http://10.0.2.2:8080` | `ws://10.0.2.2:8080` |
| 배포(Render) | `https://<서비스이름>.onrender.com` | `wss://<서비스이름>.onrender.com` |

## 인증

- 방을 만들거나(`POST /api/trips`) 참여하면(`POST /api/trips/join`) `accessToken` 을 받는다.
- 이후 REST 는 헤더 `Authorization: Bearer <accessToken>`, WebSocket 은 주소 뒤 `?token=<accessToken>`.
- GET 요청은 `?token=` 도 허용(웹 `<img>` 용).
- 토큰 하나는 방 하나에만 유효. 퇴장하면 즉시 무효(401).
- 계정 토큰(`accountToken`)은 여행방 토큰과 따로 쓴다(아래 "확장 API 초안 › 계정"). 바꿔 쓰면 403.

## 오류 응답

`{ "status": 404, "error": "Not Found", "message": "초대 코드가 없어요", "path": "..." }`
앱은 `message` 를 그대로 화면 및 음성에 사용한다.
- 입력값 검사에 걸리면 `message` 는 "입력값을 확인해 주세요", 요청 본문(JSON) 형식이 틀리면 "요청 형식이 맞지 않아요".

| 코드 | 의미 |
|---|---|
| 400 | 입력값 오류(빈 이름, 6자리가 아닌 코드 등) |
| 401 | 토큰 없음·틀림·퇴장함, 방 만들기·참여 본문의 `accountToken` 이 틀림 |
| 403 | 다른 방의 토큰, 웹 참여자가 차량 전용 기능 호출, 계정 토큰과 여행방 토큰을 바꿔 씀 |
| 404 | 없는 초대 코드, 닫힌 방 |
| 409 | 지난 제안 수락, 방 가득 참(최대 6대) |
| 503 | 서버에 LiveKit 설정 없음 |

## REST

| 메서드 | 경로 | 요청 | 응답 | 누가 |
|---|---|---|---|---|
| POST | `/api/trips` | `{name, nickname, carLabel}` | `JoinedTrip` | 차량 |
| POST | `/api/trips/join` | `{inviteCode, nickname, carLabel}` | `JoinedTrip` | 차량 |
| POST | `/api/trips/web-join` | `{inviteCode, nickname}` | `ViewerJoined` | 웹 |
| GET | `/api/trips/{tripId}` | | `Trip` | 차량·웹 |
| DELETE | `/api/trips/{tripId}/members/me` | | 204 | 차량 |
| PUT | `/api/trips/{tripId}/members/me/sharing` | `{sharing}` | 204 | 차량 |
| POST | `/api/trips/{tripId}/proposals` | `{destination}` | `DestinationProposal` | 차량 |
| POST | `/api/trips/{tripId}/proposals/{proposalId}/accept` | | 204 | 차량 |
| POST | `/api/trips/{tripId}/call-token` | | `CallCredentials` | 차량 |
| POST | `/api/trips/{tripId}/photos` | multipart `file` | `Photo` | 차량·웹 |
| GET | `/api/trips/{tripId}/photos` | | `Photo[]` 최신순 | 차량·웹 |
| GET | `/api/trips/{tripId}/photos/{photoId}/file` | | 이미지 바이트 | 차량·웹 |
| DELETE | `/api/trips/{tripId}/photos/{photoId}` | | 204 (올린 사람·방장만) | 차량·웹 |

- `inviteCode`: 6자리 영문 대문자+숫자(0·O·1·I 제외). 소문자로 보내도 된다.
- `nickname`·`carLabel`: 1~20자. `name`(방 이름): 1~30자.

## WebSocket 
`/ws/trips/{tripId}?token=...`
- 접속하면 서버가 `snapshot` 을 먼저 보낸다. 재접속해도 똑같이 보내므로 앱은 끊긴 동안의 이벤트를 따로 맞추지 않는다.
- 토큰이 틀리면 서버가 바로 닫는다(close code 1008).
- 퇴장한 차량·닫힌 방의 연결은 서버가 닫는다(close code 1000).
- 모든 메시지는 `type` 으로 구분한다.

| type | 방향 | 내용                                                                |
|---|---|---------------------------------------------------------------------|
| `snapshot` | 서버→앱·웹 | `{trip, locations: MemberLocation[]}` (2분 넘은 위치는 빠짐)        |
| `member_joined` | 서버→앱·웹 | `{member}`                                                          |
| `member_left` | 서버→앱·웹 | `{memberId, hostId}` (hostId 는 퇴장 후 방장)                       |
| `location` | 앱→서버 | `{location: {location: LatLng, headingDeg?, speedKph?}}` 1~3초 간격 |
| `location` | 서버→앱·웹 | `{location: MemberLocation}` (서버가 memberId·시각을 채움)          |
| `location_sharing` | 서버→앱·웹 | `{memberId, sharing}`. false 면 그 차의 위치를 지도에서 지운다      |
| `destination_proposed` | 서버→앱·웹 | `{proposal}`. 기다리던 제안은 무효                                  |
| `destination_accepted` | 서버→앱·웹 | `{proposalId, memberId}`                                            |
| `destination_confirmed` | 서버→앱·웹 | `{destination}`. 각 앱이 자기 내비에 길 안내 요청                   |
| `photo_added` | 서버→앱·웹 | `{photo}`                                                           |
| `photo_deleted` | 서버→앱·웹 | `{photoId}`                                                         |
| `trip_closed` | 서버→앱·웹 | `{tripId}`. 마지막 차량이 나감                                      |

- 위치는 0.5초보다 자주 보내면 서버가 버린다. 공유를 끈 차량의 위치도 버린다.

## 모델

| 이름 | 모양 |
|---|---|
| LatLng | `{lat, lng}` |
| Destination | `{name, location: LatLng}` |
| Member | `{id, nickname, carLabel, sharingLocation}` |
| DestinationProposal | `{id, proposerId, destination, acceptedBy: string[]}` |
| Trip | `{id, name, inviteCode, hostId, members: Member[], destination: Destination \| null, proposal: DestinationProposal \| null}` |
| MemberLocation | `{memberId, location: LatLng, headingDeg?, speedKph?, updatedAtMillis}` |
| JoinedTrip | `{trip, memberId, accessToken}` |
| ViewerJoined | `{trip, displayName, accessToken}` |
| CallCredentials | `{url, token}` |
| Photo | `{id, uploaderName, contentType, sizeBytes, createdAtMillis, fileUrl}` (fileUrl 은 서버 주소 뒤에 붙는 경로) |

- 값이 없는 칸은 `null` 로 온다(빠지지 않는다).
- 웹 참여자는 `members` 에 들어가지 않고 목적지 수락 인원에도 세지 않는다.

## 통화

여행방 하나 = LiveKit 방 `trip-{tripId}` 하나. 토큰의 identity 는 memberId, name 은 carLabel.
앱은 identity 로 통화 참가자와 일행 목록을 맞춘다. 토큰 유효 시간 6시간(접속할 때만 검사).

---

## 확장 API 초안

> 기준: VEHICLE_UI.md` "서버/팀 계약 요구"와 앱 시연 화면(S02\~S04, S16\~S17, S21\~S30, E05, E07\~E10) <br>
> 제안값: 만료 시간·반경 같은 숫자

### 계정 (기기 계정)

- 앱 첫 실행 시 닉네임·차량 이름으로 생성하며 비밀번호는 없다. `accountToken` 은 앱이 기기에 암호화하여 저장한다.
- 계정 토큰과 여행방 토큰(`accessToken`)은 다르다. 계정 토큰은 `/api/me/**`, `/api/friends/**`, `/api/blocks/**`, `/api/nearby/**`, `/ws/me` 에만 사용한다. 헤더 형식은 같다(`Authorization: Bearer <accountToken>`).
- 기존 방 만들기·참여(`POST /api/trips`, `POST /api/trips/join`)는 그대로 동작한다. 본문에 `accountToken`(선택)을 넣으면 그 멤버가 계정과 연결되어 친구 초대를 보낼 수 있다.

| 메서드 | 경로 | 요청 | 응답 |
|---|---|---|---|
| POST | `/api/accounts` | `{nickname, carLabel}` | `{account: Account, accountToken}` |
| GET | `/api/me` | | `Account` |
| PUT | `/api/me` | `{nickname, carLabel}` | `Account` |

### 친구·차단·초대 (S02~S06, E05)

- 친구 코드(6자리, 초대 코드와 같은 글자 규칙)로 요청 → 상대가 수락해야 친구. 요청 유효기간 7일.
- 차단하면 그 계정과의 친구 요청·주변 요청·주변 목록 노출이 양쪽 모두 막힌다. 친구였다면 친구 관계도 끊긴다.
- 여행방 초대는 친구에게만 보낼 수 있고 30분 뒤 만료된다(E05). 받은 사람은 기존 `POST /api/trips/join` 에 초대의 `inviteCode` 로 참여한다.

| 메서드 | 경로 | 요청 | 응답 |
|---|---|---|---|
| GET | `/api/friends` | | `{friends: Friend[], incoming: FriendRequest[], outgoing: FriendRequest[]}` |
| POST | `/api/friends/requests` | `{friendCode}` | `FriendRequest` |
| POST | `/api/friends/requests/{requestId}/accept` | | `Friend` |
| POST | `/api/friends/requests/{requestId}/reject` | | 204 |
| DELETE | `/api/friends/requests/{requestId}` | | 204 (보낸 사람 취소) |
| DELETE | `/api/friends/{accountId}` | | 204 |
| GET | `/api/blocks` | | `AccountSummary[]` |
| POST | `/api/blocks` | `{accountId}` | 204 |
| DELETE | `/api/blocks/{accountId}` | | 204 |
| POST | `/api/trips/{tripId}/invites` | `{accountId}` (헤더는 여행방 토큰) | `TripInvite` |

### 주변 차 탐색 (S21\~S30, E07\~E10)

- 켜 둔 동안만 탐색 대상이 된다. 앱은 켜 둔 동안 10~30초마다 `PUT /api/nearby/me` 로 위치를 갱신한다.
- 같은 목적지 판정(장소 ID 정규화): 양쪽 다 `placeId` 가 있으면 앞뒤 공백 제거·소문자로 비교한다. 하나라도 없으면 목적지 좌표 사이 거리 150m 이내를 같은 목적지로 본다.
- 후보 조건: 서로 탐색 켜짐, 같은 목적지, 두 차량 사이 거리 ≤ 내 `radiusM`, 위치 갱신 2분 이내, 서로 차단 아님, 이미 친구 아님.
- 연결 전에는 정밀 위치를 주지 않는다. 거리는 100m 단위로 올려서 준다.
- 요청 유효기간 3분(E09). 수락하면 두 계정이 친구가 된다(S28 → S05 동행 만들기).
- 탐색을 끄거나 목적지를 바꾸면 내가 보낸 대기 요청은 취소된다.

| 메서드 | 경로 | 요청 | 응답 |
|---|---|---|---|
| PUT | `/api/nearby/me` | `{destination: NearbyDestination, radiusM: 100 또는 500 또는 1000, location: LatLng}` | `NearbyStatus` |
| DELETE | `/api/nearby/me` | | 204 (S29 찾기 끄기) |
| GET | `/api/nearby/candidates` | | `NearbyCandidate[]` (가까운 순, 최대 20) |
| POST | `/api/nearby/requests` | `{toAccountId}` | `NearbyRequest` |
| POST | `/api/nearby/requests/{requestId}/accept` | | `Friend` |
| POST | `/api/nearby/requests/{requestId}/reject` | | 204 |
| DELETE | `/api/nearby/requests/{requestId}` | | 204 (보낸 사람 취소) |

| 상황 | 응답 |
|---|---|
| 탐색이 꺼진 상태에서 후보 조회 | 409 "주변 찾기가 꺼져 있어요" |
| 내 위치가 2분 넘게 갱신되지 않음 (E08) | 409 "현재 위치를 확인할 수 없어요" |
| 후보 없음 (E07) | 200 `[]` |
| 만료·취소된 요청 수락 (E09) | 409 "이 요청을 연결할 수 없어요" |
| 차단 관계 (E10) | 403 |

### 계정 실시간 연결 `/ws/me?token=<accountToken>`

- 여행방 밖(홈 화면)에서 받는 알림용. 접속하면 서버가 `me_snapshot` 을 먼저 보낸다. 재접속 규칙은 여행방 WebSocket 과 같다.

| type | 내용 |
|---|---|
| `me_snapshot` | `{account, friends, incoming, outgoing, nearbyIncoming: NearbyRequest[], invites: TripInvite[]}` |
| `friend_requested` | `{request: FriendRequest}` |
| `friend_request_closed` | `{requestId, reason}` (reason: `accepted`·`rejected`·`cancelled`·`expired`) |
| `friend_added` / `friend_removed` | `{friend: Friend}` / `{accountId}` |
| `nearby_requested` | `{request: NearbyRequest}` |
| `nearby_request_closed` | `{requestId, reason}` |
| `trip_invited` | `{invite: TripInvite}` |

### 음악 함께 듣기 (S16~S17)

- 서버는 음원을 저장·전송하지 않는다. 곡 정보와 재생 상태만 같은 여행방에 맞춘다. 음원 공급(사용 허가 받은 곡 목록·재생)은 앱 쪽에서 정한다.
- 상태는 서버 메모리에만 둔다(방이 닫히면 사라짐). 여행방 토큰(차량)만 쓸 수 있다.
- 요청 유효기간 1분. 한 명이라도 수락하면 재생을 시작한 것으로 보고 `music_state` 를 보낸다.

| 메서드 | 경로 | 요청 | 응답 |
|---|---|---|---|
| POST | `/api/trips/{tripId}/music/requests` | `{track: Track}` | `MusicRequest` |
| POST | `/api/trips/{tripId}/music/requests/{requestId}/accept` | | 204 |
| DELETE | `/api/trips/{tripId}/music/requests/{requestId}` | | 204 (요청자는 취소, 다른 사람은 거절) |
| PUT | `/api/trips/{tripId}/music/state` | `{track, playing, positionMs}` | `MusicState` |

- 여행방 WebSocket 이벤트 추가: `music_requested {request}`, `music_request_closed {requestId, reason}`, `music_state {state}`. `snapshot` 에 `music: MusicState | null` 이 추가된다.
- 시간 맞추기: 앱은 `재생 위치 = positionMs + (지금 - updatedAtMillis)` 로 계산하고, 기기 시각 차이는 `serverTimeMillis` 로 보정한다.

### 모델 추가

| 이름 | 모양 |
|---|---|
| Account | `{id, nickname, carLabel, friendCode}` |
| AccountSummary | `{id, nickname, carLabel}` |
| Friend | `{account: AccountSummary, sinceMillis, origin}` (origin: `code`·`nearby`) |
| FriendRequest | `{id, from: AccountSummary, to: AccountSummary, createdAtMillis, expiresAtMillis}` |
| TripInvite | `{id, tripId, tripName, inviteCode, from: AccountSummary, expiresAtMillis}` |
| NearbyDestination | `{placeId: string \| null, name, location: LatLng}` |
| NearbyStatus | `{enabled, destination, radiusM, locationUpdatedAtMillis}` |
| NearbyCandidate | `{account: AccountSummary, destinationName, distanceM}` |
| NearbyRequest | `{id, from: AccountSummary, to: AccountSummary, distanceM, expiresAtMillis}` |
| Track | `{id, title, artist, durationMs}` |
| MusicRequest | `{id, requesterId, track, expiresAtMillis}` |
| MusicState | `{track, playing, positionMs, updatedAtMillis, serverTimeMillis, controllerId}` |
