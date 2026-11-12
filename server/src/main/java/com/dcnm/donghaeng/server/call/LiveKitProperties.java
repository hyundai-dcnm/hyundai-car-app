package com.dcnm.donghaeng.server.call;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** LIVEKIT_URL, LIVEKIT_API_KEY, LIVEKIT_API_SECRET 환경 변수로 넣는다. 저장소에 올리지 않는다. */
@ConfigurationProperties(prefix = "livekit")
public record LiveKitProperties(String url, String apiKey, String apiSecret) {

    boolean configured() {
        return notBlank(url) && notBlank(apiKey) && notBlank(apiSecret);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}