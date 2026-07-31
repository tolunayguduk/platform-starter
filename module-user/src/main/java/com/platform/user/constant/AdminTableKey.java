package com.platform.user.constant;

/** The MySQL "main tables" the admin panel's database browser can inspect - deliberately a fixed
 * whitelist, never a client-supplied table name (see AdminTableServiceImpl). */
public enum AdminTableKey {
    USER_PROFILE,
    USER_CONTACT,
    USER_CONSENT,
    PERMISSION,
    ROLE_PERMISSION
}
