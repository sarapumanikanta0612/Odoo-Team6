package com.wams.enquiry;

/**
 * Lifecycle of an enquiry.
 * <p>
 * {@link #CONVERTED} is set only when a wedding project is created from the enquiry
 * (after the agreement is signed and the booking deposit is paid). The public form
 * cannot set any status, and manual status changes must never set CONVERTED.
 * <p>
 * Keep this list complete: with {@code ddl-auto=update}, Hibernate creates a database
 * check constraint from these values and does not update it when values are added.
 */
public enum EnquiryStatus {
	NEW,
	CONTACTED,
	CONSULTATION,
	PROPOSAL,
	CONVERTED,
	LOST
}
