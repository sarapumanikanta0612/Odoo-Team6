package com.wams.enums;

/**
 * Lifecycle status for a WeddingProject. Only reachable after the
 * agreement + deposit creation gate has been satisfied.
 */
public enum ProjectStatus {
    PLANNING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    POSTPONED
}
