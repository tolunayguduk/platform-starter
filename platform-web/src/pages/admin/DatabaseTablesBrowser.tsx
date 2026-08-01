import { useEffect, useState } from 'react';
import { App, Button, Card, DatePicker, Descriptions, Form, Input, InputNumber, Modal, Select, Table, Tabs, Typography } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../auth/AuthContext';
import { ApiError } from '../../api/client';
import {
  fetchAdminTableAuditRows,
  fetchAdminTableRows,
  fetchAdminTables,
  updateAdminTableRow,
  type AdminTable,
} from '../../api/admin';

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

// The only editable columns across all five tables that aren't plain text - everything else
// falls back to a text Input.
const DATE_FIELDS = new Set(['birth_date']);
const ENUM_FIELD_OPTIONS: Record<string, string[]> = {
  ui_policy: ['HIDE_IF_DENIED', 'DISABLE_IF_DENIED'],
  access_level: ['GRANTED', 'VISIBLE_DENIED'],
};
const NUMBER_FIELDS = new Set(['permission_id']);

function renderFieldInput(field: string) {
  if (DATE_FIELDS.has(field)) return <DatePicker style={{ width: '100%' }} />;
  if (ENUM_FIELD_OPTIONS[field]) {
    return <Select options={ENUM_FIELD_OPTIONS[field].map((v) => ({ value: v, label: v }))} />;
  }
  if (NUMBER_FIELDS.has(field)) return <InputNumber style={{ width: '100%' }} />;
  return <Input />;
}

function toFormValue(field: string, value: unknown): unknown {
  if (DATE_FIELDS.has(field)) return value ? dayjs(value as string) : null;
  return value ?? undefined;
}

function fromFormValue(field: string, value: unknown): unknown {
  if (DATE_FIELDS.has(field)) return value ? (value as Dayjs).format('YYYY-MM-DD') : null;
  if (value === undefined || value === '') return null;
  return value;
}

interface EditRowModalProps {
  open: boolean;
  tableKey: string;
  primaryKeyColumn: string;
  editableColumns: string[];
  row: Record<string, unknown>;
  onClose: () => void;
  onSaved: (updatedRow: Record<string, unknown>) => void;
}

function EditRowModal({ open, tableKey, primaryKeyColumn, editableColumns, row, onClose, onSaved }: EditRowModalProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [form] = Form.useForm();
  const [mode, setMode] = useState<'edit' | 'confirm'>('edit');
  const [pendingChanges, setPendingChanges] = useState<Record<string, unknown>>({});
  const [saving, setSaving] = useState(false);

  function handleClose() {
    setMode('edit');
    setPendingChanges({});
    onClose();
  }

  function handleReview() {
    const values = form.getFieldsValue();
    const changes: Record<string, unknown> = {};
    for (const field of editableColumns) {
      const newValue = fromFormValue(field, values[field]);
      const oldValue = row[field] ?? null;
      if (newValue !== oldValue) {
        changes[field] = newValue;
      }
    }
    setPendingChanges(changes);
    setMode('confirm');
  }

  async function handleConfirm() {
    if (!accessToken) return;
    setSaving(true);
    try {
      const updated = await updateAdminTableRow(accessToken, tableKey, String(row[primaryKeyColumn]), pendingChanges);
      message.success(t('admin.editRow.success'));
      onSaved(updated);
      handleClose();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.editRow.error')) : t('admin.editRow.error'));
    } finally {
      setSaving(false);
    }
  }

  const hasChanges = Object.keys(pendingChanges).length > 0;

  return (
    <Modal
      open={open}
      onCancel={handleClose}
      destroyOnHidden
      title={mode === 'edit' ? t('admin.editRow.title') : t('admin.editRow.confirmTitle')}
      footer={
        mode === 'edit'
          ? [
              <Button key="cancel" onClick={handleClose}>
                {t('admin.editRow.cancel')}
              </Button>,
              <Button key="save" type="primary" onClick={handleReview}>
                {t('admin.editRow.save')}
              </Button>,
            ]
          : [
              <Button key="back" onClick={() => setMode('edit')}>
                {t('admin.editRow.back')}
              </Button>,
              <Button key="confirm" type="primary" loading={saving} disabled={!hasChanges} onClick={handleConfirm}>
                {t('admin.editRow.confirm')}
              </Button>,
            ]
      }
    >
      {mode === 'edit' ? (
        <Form
          form={form}
          layout="vertical"
          initialValues={Object.fromEntries(editableColumns.map((field) => [field, toFormValue(field, row[field])]))}
        >
          {editableColumns.map((field) => (
            <Form.Item key={field} name={field} label={prettifyColumn(field)}>
              {renderFieldInput(field)}
            </Form.Item>
          ))}
        </Form>
      ) : hasChanges ? (
        <Descriptions column={1} bordered size="small">
          {Object.entries(pendingChanges).map(([field, newValue]) => (
            <Descriptions.Item key={field} label={prettifyColumn(field)}>
              <span style={{ textDecoration: 'line-through', opacity: 0.6 }}>{formatCellValue(row[field])}</span>
              {' → '}
              <strong>{formatCellValue(newValue)}</strong>
            </Descriptions.Item>
          ))}
        </Descriptions>
      ) : (
        <Typography.Text type="secondary">{t('admin.editRow.noChanges')}</Typography.Text>
      )}
    </Modal>
  );
}

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

function AdminTableTab({ tableKey, hasAudit, editableColumns }: { tableKey: string; hasAudit: boolean; editableColumns: string[] }) {
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
        dataSource={rows}
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
          children: <AdminTableTab tableKey={table.key} hasAudit={table.hasAudit} editableColumns={table.editableColumns} />,
        }))}
      />
    </Card>
  );
}
