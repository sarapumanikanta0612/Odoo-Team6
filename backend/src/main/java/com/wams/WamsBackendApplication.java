package com.wams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class WamsBackendApplication {

	public static void main(String[] args) {
		// WAMS is a single-region (India/INR/English) application per SRS §1.
		// Fix the JVM default to Asia/Kolkata BEFORE Spring (and therefore pgjdbc)
		// reads TimeZone.getDefault() when opening the first connection. On Windows,
		// Java 21 reports Asia/Calcutta by default, which PostgreSQL 17 rejects
		// (the legacy IANA alias was dropped from the server's accepted set).
		//
		// This runs regardless of launch method (./mvnw spring-boot:run, java -jar,
		// IDE Run, Docker), so no developer needs to remember a special -D flag.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

		SpringApplication.run(WamsBackendApplication.class, args);
	}

}
