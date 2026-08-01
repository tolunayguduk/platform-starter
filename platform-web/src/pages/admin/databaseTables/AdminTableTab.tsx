import { useEffect, useState } from 'react';
import { Button, Table } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { fetchAdminTableRows } from '../../../api/adminApi';
import { buildColumns } from './tableFormat';
import { AuditRowsView } from './AuditRowsView';
import { EditRowModal } from './EditRowModal';

export function AdminTableTab({
  tableKey,
  hasAudit,
  editableColumns,
  filterUserId,
}: {
  tableKey: string;
  hasAudit: boolean;
  editableColumns: string[];
  filterUserId: string;
}) {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const [columns, setColumns] = useState<string[]>([]);
  const [primaryKeyColumn, setPrimaryKeyColumn] = useState('');
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingRow, setEditingRow] = useState<Record<string, unknown> | null>(null);

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

  // The table is fetched in full (same 500-row cap as before) and filtered here - no extra
  // endpoint needed, and switching the selected user re-filters without a re-fetch.
  const visibleRows = rows.filter((r) => String(r.keycloak_user_id) === filterUserId);

  function handleSaved(updatedRow: Record<string, unknown>) {
    setRows((prev) => prev.map((r) => (r[primaryKeyColumn] === updatedRow[primaryKeyColumn] ? updatedRow : r)));
  }

  const tableColumns = [
    ...buildColumns(columns),
    {
      title: t('admin.editRow.actionsColumn'),
      key: 'actions',
      render: (_: unknown, record: Record<string, unknown>) => (
        <Button size="small" onClick={() => setEditingRow(record)}>
          {t('admin.editRow.editAction')}
        </Button>
      ),
    },
  ];

  return (
    <>
      <Table
        rowKey={(record) => String(record[primaryKeyColumn])}
        loading={loading}
        dataSource={visibleRows}
        columns={tableColumns}
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
      {editingRow && (
        <EditRowModal
          open
          tableKey={tableKey}
          primaryKeyColumn={primaryKeyColumn}
          editableColumns={editableColumns}
          row={editingRow}
          onClose={() => setEditingRow(null)}
          onSaved={handleSaved}
        />
      )}
    </>
  );
}
