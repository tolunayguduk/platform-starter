// Every function is one of exactly these three statuses for a given role - see AccessLevel.
export const ACCESS_LEVEL_OPTIONS = ['GRANTED', 'VISIBLE_DENIED', 'HIDDEN'];

// A new function's fallback behavior for any role that has no explicit status set - see UiPolicy.
export const UI_POLICY_OPTIONS = ['HIDE_IF_DENIED', 'DISABLE_IF_DENIED'];

// Whether a role's authority reaches into the admin panel platform-wide - see RoleScope. Not
// name-based: which role (if any) is "the" platform admin is entirely data-driven, editable from
// this same table.
export const ROLE_SCOPE_OPTIONS = ['NONE', 'PLATFORM'];
