// Every function is one of exactly these three statuses for a given role - see AccessLevel.
export const ACCESS_LEVEL_OPTIONS = ['GRANTED', 'VISIBLE_DENIED', 'HIDDEN'];

// A new function's fallback behavior for any role that has no explicit status set - see UiPolicy.
export const UI_POLICY_OPTIONS = ['HIDE_IF_DENIED', 'DISABLE_IF_DENIED'];

// The role every admin action in this app is gated on - deleting it would lock everyone out of
// ever managing roles/functions again. Rejected server-side too; disabled here for a clearer UX.
export const PROTECTED_ROLE = 'ADMIN';
