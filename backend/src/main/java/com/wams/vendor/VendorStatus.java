package com.wams.vendor;

/**
 * SRS 5.5 / 6.8: admin-added vendors are trusted (APPROVED);
 * self-registered vendors wait for admin approval.
 * All values are declared now because Hibernate writes a CHECK constraint
 * from this list and ddl-auto=update will not change it later.
 */
public enum VendorStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED
}
