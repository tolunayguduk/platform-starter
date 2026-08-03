package com.platform.user.constant;

/** Which direction an organization_membership_request came from - see OrganizationMembershipRequest. */
public enum MembershipRequestType {
    /** A manager invited this user to their organization - the user must accept it. */
    INVITE,
    /** This user asked to join an organization themselves (via its permanent invite link) - the
     * organization's manager must approve it, unless that organization's membershipRequiresApproval
     * setting is off, in which case it's auto-approved at creation time. */
    JOIN_REQUEST
}
