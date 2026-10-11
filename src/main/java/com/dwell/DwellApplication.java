package com.dwell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DwellApplication {

	public static void main(String[] args) {
		// 서비스 기준 시간대는 뉴욕 (실행 환경과 무관하게 LocalDateTime 저장·응답을 뉴욕 시각으로 통일)
		TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
		SpringApplication.run(DwellApplication.class, args);
	}

}
