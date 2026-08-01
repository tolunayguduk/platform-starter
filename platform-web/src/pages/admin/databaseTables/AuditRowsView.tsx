import { useEffect, useState } from 'react';
import { Table } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { fetchAdminTableAuditRows } from '../../../api/adminApi';
import { buildColumns, REV_TYPE_KEYS } from './tableFormat';

export function AuditRowsView({ tableKey, primaryKeyValue }: { tableKey: string; primaryKeyValue: string }) {
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
