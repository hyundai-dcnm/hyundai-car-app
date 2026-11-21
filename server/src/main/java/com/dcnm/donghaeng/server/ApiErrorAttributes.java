package com.dcnm.donghaeng.server;

import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

/**
 * 오류 응답 {status, error, message, path} 의 message 를 앱에 바로 보여 줄 수 있는 한국어로 맞춘다.
 * 서버 코드가 직접 던지는 오류(ResponseStatusException)는 원래 문구 그대로 두고,
 * Spring 이 만드는 영어 기본 문구 중 앱 입력에서 생길 수 있는 2가지(입력값 검사, 본문 형식)만 바꾼다.
 */
@Component
public class ApiErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest request, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(request, options);
        String message = koreanMessage(getError(request));
        if (message != null && attributes.containsKey("message")) {
            attributes.put("message", message);
        }
        return attributes;
    }

    private static String koreanMessage(Throwable error) {
        if (error instanceof MethodArgumentNotValidException) return "입력값을 확인해 주세요";
        if (error instanceof HttpMessageNotReadableException) return "요청 형식이 맞지 않아요";
        return null;
    }
}
