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
- 계정·로그인은 아직 없다(④ 후속, 팀 합의 후).

## 오류 응답

`{ "status": 404, "error": "Not Found", "message": "초대 코드가 없어요", "path": "..." }`
앱은 `message` 를 그대로 화면 및 음성에 사용한다.

| 코드 | 의미 |
|---|---|
| 400 | 입력값 오류(빈 이름, 6자리가 아닌 코드 등) |
| 401 | 토큰 없음·틀림·퇴장함 |
| 403 | 다른 방의 토큰, 웹 참여자가 차량 전용 기능 호출 |
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