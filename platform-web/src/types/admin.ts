export interface AdminUser {
  id: string;
  username: string;
  email: string;
  fullName: string | null;
  status: string;
  createdAt: string;
  roles: string[];
}

export interface AdminRole {
  name: string;
  description: string | null;
  enabled: boolean;
}

export interface AdminUserAuditEvent {
  time: string;
  operationType: string;
  resourcePath: string;
  representation: string | null;
}

export type StatsRange = 'DAY' | 'WEEK' | 'MONTH' | 'YEAR';

export interface RegistrationStatsPoint {
  bucket: string;
  count: number;
}

export interface AdminTable {
  key: string;
  hasAudit: boolean;
  editableColumns: string[];
}

export interface AdminTableRows {
  columns: string[];
  primaryKeyColumn: string;
  rows: Record<string, unknown>[];
}

export interface AdminAuditRows {
  columns: string[];
  rows: Record<string, unknown>[];
}

/** A row from the PERMISSION table, as used by the Role-Function management screens. */
export interface PermissionCatalogEntry {
  id: number;
  key: string;
  enabled: boolean;
}

/** One ROLE_PERMISSION grant, joined with its PERMISSION catalog entry, for a single role. */
export interface FunctionGrantRow {
  grantId: string;
  permissionId: number;
  functionKey: string;
  accessLevel: string;
  functionEnabled: boolean;
}

/** A row in the global function catalog (independent of any one role). */
export interface FunctionCatalogRow {
  id: string;
  key: string;
  uiPolicy: string;
  description: string | null;
  enabled: boolean;
}

/** One (function, role) grant for a specific user - see UserFunctionAccessView. */
export interface UserFunctionGrantRow {
  rowKey: string;
  functionKey: string;
  description: string | null;
  role: string;
  accessLevel: string;
}
