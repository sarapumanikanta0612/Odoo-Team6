package com.wams.enquiry;

/**
 * How an enquiry reached the agency.
 * <p>
 * The public website form always records {@link #WEBSITE}. The other values are for
 * enquiries entered by staff.
 * <p>
 * Keep this list complete: with {@code ddl-auto=update}, Hibernate creates a database
 * check constraint from these values and does not update it when values are added.
 */
public enum EnquirySource {
	WEBSITE,
	PHONE,
	EMAIL,
	WALK_IN,
	REFERRAL,
	SOCIAL_MEDIA,
	OTHER
}
