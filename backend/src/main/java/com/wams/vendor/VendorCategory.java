package com.wams.vendor;

/**
 * Service categories for the vendor directory and for quotation requests.
 * Proposed list: the SRS does not define vendor categories.
 * VENUE and TRANSPORT are left out on purpose because venue booking and
 * logistics are outside the MVP (SRS 6.18 F, 6.22 F).
 */
public enum VendorCategory {
    CATERING,
    DECOR,
    PHOTOGRAPHY,
    MAKEUP,
    MEHENDI,
    MUSIC_ENTERTAINMENT,
    LIGHTING_SOUND,
    INVITATIONS,
    OTHER
}
