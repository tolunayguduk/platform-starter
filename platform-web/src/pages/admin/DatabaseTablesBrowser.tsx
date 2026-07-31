import { useEffect, useState } from 'react';
import { Card, Table, Tabs, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../auth/AuthContext';
import { fetchAdminTableAuditRows, fetchAdminTableRows, fetchAdminTables, type AdminTable } from '../../api/admin';

/** "USER_PROFILE" -> "userProfile", to key into the admin.tables.* i18n namespace. */
function toCamelKey(enumKey: string): string {
  return enumKey.toLowerCase().replace(/_([a-z])/g, (_, c: string) => c.toUpperCase());
}

/** "full_name" -> "Full Name" - good enough for a raw-table debug view, not worth a translation
 * entry per column across every table. */
function prettifyColumn(column: string): string {
  return column.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
}

function formatCellValue(value: unknown): string {
  if (value === null || value === undefined) return '-';
  if (typeof value === 'boolean') return value ? 'true' : 'false';
  return String(value);
}

function buildColumns(columns: string[]) {
  return columns.map((column) => ({
    title: prettifyColumn(column),
    dataIndex: column,
    key: column,
    render: formatCellValue,
  }));
}

const REV_TYPE_KEYS: Record<string, string> = { '0': 'added', '1': 'modified', '2': 'deleted' };

function AuditRowsView({ tableKey, primaryKeyValue }: { tableKey: string; primaryKeyValue: string }) {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const [columns, setColumns] = useState<string[]>([]);
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!accessToken) return;
    setLoading(true);
    fetchAdminTableAuditRows(accessToken, tableKey, primaryKeyValue)
      .then((data) => {
        setColumns(data.columns);
        setRows(data.rows);
      })
      .finally(() => setLoading(false));
  }, [accessToken, tableKey, primaryKeyValue]);

  const tableColumns = buildColumns(columns).map((col) =>
    col.dataIndex === 'revtype'
      ? { ...col, render: (value: unknown) => t(`admin.auditRevType.${REV_TYPE_KEYS[String(value)] ?? 'unknown'}`) }
      : col,
  );

  return (
    <Table
      size="small"
      rowKey={(record) => String(record.rev)}
      loading={loading}
      dataSource={rows}
      columns={tableColumns}
      pagination={false}
    />
  );
}

function AdminTableTab({ tableKey, hasAudit }: { tableKey: string; hasAudit: boolean }) {
  const { accessToken } = useAuth();
  const [columns, setColumns] = useState<string[]>([]);
  const [primaryKeyColumn, setPrimaryKeyColumn] = useState('');
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!accessToken) return;
    setLoading(true);
    fetchAdminTableRows(accessToken, tableKey)
      .then((data) => {
        setColumns(data.columns);
        setPrimaryKeyColumn(data.primaryKeyColumn);
        setRows(data.rows);
      })
      .finally(() => setLoading(false));
  }, [accessToken, tableKey]);

  return (
    <Table
      rowKey={(record) => String(record[primaryKeyColumn])}
      loading={loading}
      dataSource={rows}
      columns={buildColumns(columns)}
      pagination={{ pageSize: 10 }}
      expandable={
        hasAudit
          ? {
              expandedRowRender: (record) => (
                <AuditRowsView tableKey={tableKey} primaryKeyValue={String(record[primaryKeyColumn])} />
              ),
            }
          : undefined
      }
    />
  );
}

export function DatabaseTablesBrowser() {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const [tables, setTables] = useState<AdminTable[]>([]);

  useEffect(() => {
    if (!accessToken) return;
    fetchAdminTables(accessToken).then(setTables);
  }, [accessToken]);

  return (
    <Card title={t('admin.databaseTablesTitle')}>
      <Typography.Paragraph type="secondary">{t('admin.databaseTablesHint')}</Typography.Paragraph>
      <Tabs
        items={tables.map((table) => ({
          key: table.key,
          label: t(`admin.tables.${toCamelKey(table.key)}`),
          children: <AdminTableTab tableKey={table.key} hasAudit={table.hasAudit} />,
        }))}
      />
    </Card>
  );
}
