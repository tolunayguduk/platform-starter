import dayjs, { type Dayjs } from 'dayjs';

// Same three statuses/colors as the Role-Function tab - these are the function's *actual* grants
// for one of the user's roles, not a merged "what wins" computation.
export const ACCESS_LEVEL_COLOR: Record<string, string> = { GRANTED: 'green', VISIBLE_DENIED: 'orange', HIDDEN: 'default' };

/** PERMISSION/ROLE_PERMISSION are global definitions, not scoped to any one user - they're
 * represented instead by the computed Function Access tab (see UserFunctionAccessView) rather
 * than as raw, editable tables in this per-user view. */
export const RAW_TABLE_KEYS = new Set(['USER_PROFILE', 'USER_CONTACT', 'USER_CONSENT']);

export const REV_TYPE_KEYS: Record<string, string> = { '0': 'added', '1': 'modified', '2': 'deleted' };

// The only editable column across the three user-scoped tables that isn't plain text -
// everything else falls back to a text Input.
export const DATE_FIELDS = new Set(['birth_date']);

/** "USER_PROFILE" -> "userProfile", to key into the admin.tables.* i18n namespace. */
export function toCamelKey(enumKey: string): string {
  return enumKey.toLowerCase().replace(/_([a-z])/g, (_, c: string) => c.toUpperCase());
}

/** "full_name" -> "Full Name" - good enough for a raw-table debug view, not worth a translation
 * entry per column across every table. */
export function prettifyColumn(column: string): string {
  return column.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
}

export function formatCellValue(value: unknown): string {
  if (value === null || value === undefined) return '-';
  if (typeof value === 'boolean') return value ? 'true' : 'false';
  return String(value);
}

export function buildColumns(columns: string[]) {
  return columns.map((column) => ({
    title: prettifyColumn(column),
    dataIndex: column,
    key: column,
    render: formatCellValue,
  }));
}

export function toFormValue(field: string, value: unknown): unknown {
  if (DATE_FIELDS.has(field)) return value ? dayjs(value as string) : null;
  return value ?? undefined;
}

export function fromFormValue(field: string, value: unknown): unknown {
  if (DATE_FIELDS.has(field)) return value ? (value as Dayjs).format('YYYY-MM-DD') : null;
  if (value === undefined || value === '') return null;
  return value;
}
