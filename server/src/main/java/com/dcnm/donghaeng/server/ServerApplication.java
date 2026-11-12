package com.dcnm.donghaeng.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** 서버 시작점. @ConfigurationPropertiesScan 은 livekit.*, storage.* 설정을 읽게 해줌 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
	}

}