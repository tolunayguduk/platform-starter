import { useState } from 'react';
import { App, Form, Input, Modal } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { createAdminRole, createAdminTableRow, fetchAdminTableRows } from '../../../api/adminApi';

interface CopyRoleModalProps {
  open: boolean;
  sourceRole: string;
  onClose: () => void;
  onCopied: () => void;
}

/** Creates a new role, then copies every function grant the source role has onto it (same
 * function, same access level). A grant that fails to copy - e.g. the function it points to is
 * disabled - is reported but doesn't block the rest; the new role still ends up created either
 * way. */
export function CopyRoleModal({ open, sourceRole, onClose, onCopied }: CopyRoleModalProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [form] = Form.useForm<{ name: string }>();
  const [saving, setSaving] = useState(false);

  function handleClose() {
    form.resetFields();
    onClose();
  }

  async function handleSubmit() {
    if (!accessToken) return;
    const { name } = await form.validateFields();
    const newName = name.trim();
    setSaving(true);
    try {
      await createAdminRole(accessToken, newName);
      const rolePermissions = await fetchAdminTableRows(accessToken, 'ROLE_PERMISSION');
      const grants = rolePermissions.rows.filter((r) => r.role_name === sourceRole);
      const results = await Promise.allSettled(
        grants.map((g) =>
          createAdminTableRow(accessToken, 'ROLE_PERMISSION', {
            role_name: newName,
            permission_id: g.permission_id,
            access_level: g.access_level,
          }),
        ),
      );
      const failures = results.filter((r) => r.status === 'rejected').length;
      if (failures > 0) {
        message.warning(t('admin.roleFunctions.copyRole.partialError', { count: failures }));
      } else {
        message.success(t('admin.roleFunctions.copyRole.success'));
      }
      handleClose();
      onCopied();
    } catch (e) {
      message.error(
        e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.copyRole.error')) : t('admin.roleFunctions.copyRole.error'),
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      open={open}
      title={t('admin.roleFunctions.copyRole.title', { role: sourceRole })}
      onCancel={handleClose}
      onOk={handleSubmit}
      confirmLoading={saving}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="name" label={t('admin.roleFunctions.copyRole.placeholder')} rules={[{ required: true, whitespace: true }]}>
          <Input placeholder={t('admin.roleFunctions.copyRole.placeholder')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
