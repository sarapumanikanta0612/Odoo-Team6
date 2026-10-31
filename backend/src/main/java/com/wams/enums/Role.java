package com.wams.enums;

/**
 * Registered user categories supported by WAMS.
 *
 * PUBLIC_VISITOR is an unauthenticated category and intentionally NOT included
 * here: no User record should be created for anonymous visitors.
 */
public enum Role {
    ADMIN,
    LEAD_PLANNER,
    EMPLOYEE,
    PRIMARY_CLIENT,
    PARTNER_CLIENT,
    VENDOR
}
