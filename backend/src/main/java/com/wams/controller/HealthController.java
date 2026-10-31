package com.wams.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Preserved existing endpoint. Does NOT verify database connectivity.
 * The response does not imply the database is reachable; that is a separate concern.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String health() {
        return "WAMS Backend is running successfully!";
    }
}
