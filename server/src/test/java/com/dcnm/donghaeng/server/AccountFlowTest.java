package com.dcnm.donghaeng.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dcnm.donghaeng.server.account.AccountService;
import com.dcnm.donghaeng.server.trip.MemberRepository;
import com.dcnm.donghaeng.server.trip.TripService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

/**
 * 계정 규칙 확인. 실제 주소로 요청을 보내 토큰 종류별 401/403 도 확인한다.
 * 테스트 DB 는 src/test/resources/application.properties 에서 정한다(내 컴퓨터는 메모리 DB, CI 는 PostgreSQL).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountFlowTest {

    @Autowired
    AccountService accounts;

    @Autowired
    TripService trips;

    @Autowired
    MemberRepository members;

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void accountCanReadAndUpdateItself() {
        var created = accounts.create(" 민지 ", "민지네 차");
        assertThat(created.accountToken()).hasSize(43);
        assertThat(created.account().nickname()).isEqualTo("민지");
        assertThat(created.account().friendCode()).matches("[A-HJ-NP-Z2-9]{6}");

        var me = accounts.resolve(created.accountToken()).orElseThrow();
        assertThat(accounts.me(me).carLabel()).isEqualTo("민지네 차");

        var updated = accounts.update(me, "민지", "새 차");
        assertThat(updated.carLabel()).isEqualTo("새 차");
        assertThat(updated.friendCode()).isEqualTo(created.account().friendCode());
    }

    @Test
    void memberIsLinkedOnlyWhenAccountTokenIsGiven() {
        var account = accounts.create("민지", "민지네 차");
        var host = trips.create("강릉 여행", "민지", "민지네 차", accounts.accountIdForLink(account.accountToken()));
        var guest = trips.join(host.trip().inviteCode(), "규원", "규원이 차", accounts.accountIdForLink(null));

        assertThat(members.findById(host.memberId()).orElseThrow().getAccountId()).isEqualTo(account.account().id());
        assertThat(members.findById(guest.memberId()).orElseThrow().getAccountId()).isNull();
        // 계정 연결과 상관없이 방장 지정은 그대로여야 한다.
        assertThat(host.trip().hostId()).isEqualTo(host.memberId());
    }

    @Test
    void wrongAccountTokenCannotBeLinked() {
        assertThat(accounts.accountIdForLink(" ")).isNull();
        assertThatThrownBy(() -> accounts.accountIdForLink("not-a-real-token"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void eachTokenOnlyWorksOnItsOwnAddresses() throws Exception {
        var account = accounts.create("민지", "민지네 차");
        var host = trips.create("속초 여행", "민지", "민지네 차");
        String tripPath = "/api/trips/" + host.trip().id();

        assertThat(get("/api/me", account.accountToken())).isEqualTo(200);
        assertThat(get("/api/me", null)).isEqualTo(401);
        assertThat(get("/api/me", "not-a-real-token")).isEqualTo(401);
        assertThat(get("/api/me", host.accessToken())).isEqualTo(403);   // 여행방 토큰으로 계정 주소
        assertThat(get(tripPath, account.accountToken())).isEqualTo(403); // 계정 토큰으로 여행방 주소
        assertThat(get(tripPath, host.accessToken())).isEqualTo(200);    // 기존 기능은 그대로
    }

    @Test
    void accountCanBeCreatedOverHttp() throws Exception {
        var ok = post("/api/accounts", "{\"nickname\":\"규원\",\"carLabel\":\"규원이 차\"}");
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.body()).contains("\"accountToken\"").contains("\"friendCode\"");

        var empty = post("/api/accounts", "{\"nickname\":\"\",\"carLabel\":\"규원이 차\"}");
        assertThat(empty.statusCode()).isEqualTo(400);

        // 계정 토큰 없이 방 만들기는 예전처럼 된다(앱 1차 기능 보호).
        var trip = post("/api/trips", "{\"name\":\"양양 여행\",\"nickname\":\"규원\",\"carLabel\":\"규원이 차\"}");
        assertThat(trip.statusCode()).isEqualTo(200);

        // 틀린 계정 토큰을 넣으면 방을 만들지 않는다.
        var wrong = post("/api/trips",
                "{\"name\":\"양양 여행\",\"nickname\":\"규원\",\"carLabel\":\"규원이 차\",\"accountToken\":\"not-a-real-token\"}");
        assertThat(wrong.statusCode()).isEqualTo(401);
    }

    private int get(String path, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
