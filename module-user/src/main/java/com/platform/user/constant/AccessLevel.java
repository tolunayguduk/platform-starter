package com.platform.user.constant;

public enum AccessLevel {
    /** The role can actually perform the action - the only value MatrixPermissionEvaluator's real
     * authorization check ever sees. */
    GRANTED,
    /** UI-only: renders as visible but disabled, instead of the permission's default fallback. */
    VISIBLE_DENIED,
    /** UI-only: an explicit "this role does not see this control at all", overriding the
     * permission's own ui_policy-based default for roles that would otherwise fall through to it. */
    HIDDEN
}
