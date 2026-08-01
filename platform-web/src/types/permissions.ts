/** ENABLED -> normal button, DISABLED -> disabled button, HIDDEN -> not rendered. Mirrors the
 * old components/permission-button.html fragment, sourced from the same backend endpoint. */
export type UiPermissionState = 'ENABLED' | 'DISABLED' | 'HIDDEN';

export type UiPermissions = Record<string, UiPermissionState>;
