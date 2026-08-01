import { useState } from 'react';
import { App, Button, DatePicker, Descriptions, Form, Input, Modal, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { updateAdminTableRow } from '../../../api/adminApi';
import { DATE_FIELDS, formatCellValue, fromFormValue, prettifyColumn, toFormValue } from './tableFormat';

function renderFieldInput(field: string) {
  if (DATE_FIELDS.has(field)) return <DatePicker style={{ width: '100%' }} />;
  return <Input />;
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

export function EditRowModal({ open, tableKey, primaryKeyColumn, editableColumns, row, onClose, onSaved }: EditRowModalProps) {
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
