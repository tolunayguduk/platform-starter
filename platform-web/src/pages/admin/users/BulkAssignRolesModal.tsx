import { useState } from 'react';
import { App, Form, Modal, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { updateUserRoles } from '../../../api/adminApi';
import type { AdminRole, AdminUser } from '../../../types/admin';

interface BulkAssignRolesModalProps {
  open: boolean;
  users: AdminUser[];
  availableRoles: AdminRole[];
  onClose: () => void;
  onDone: () => void;
}

/** Additive only - picked roles are unioned onto each selected user's existing roles, never
 * replacing what they already have. A destructive bulk "set roles to exactly X" is a much easier
 * way to accidentally strip access from people outside the intended change. */
export function BulkAssignRolesModal({ open, users, availableRoles, onClose, onDone }: BulkAssignRolesModalProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [form] = Form.useForm<{ roles: string[] }>();
  const [saving, setSaving] = useState(false);

  function handleClose() {
    form.resetFields();
    onClose();
  }

  async function handleSubmit() {
    if (!accessToken) return;
    const { roles: rolesToAdd } = await form.validateFields();
    setSaving(true);
    try {
      const results = await Promise.allSettled(
        users.map((user) => updateUserRoles(accessToken, user.id, Array.from(new Set([...user.roles, ...rolesToAdd])))),
      );
      const failures = results.filter((r) => r.status === 'rejected').length;
      if (failures > 0) {
        message.warning(t('admin.bulk.partialError', { count: failures }));
      } else {
        message.success(t('admin.bulk.success'));
      }
      handleClose();
      onDone();
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      open={open}
      title={t('admin.bulk.assignRolesModal.title', { count: users.length })}
      onCancel={handleClose}
      onOk={handleSubmit}
      confirmLoading={saving}
      okText={t('admin.bulk.assignRolesModal.button')}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="roles" label={t('admin.editUser.fields.roles')} rules={[{ required: true, type: 'array', min: 1 }]}>
          <Select
            mode="multiple"
            placeholder={t('admin.bulk.assignRolesModal.placeholder')}
            options={availableRoles.map((r) => ({
              value: r.name,
              label: r.enabled ? r.name : t('admin.editUser.roleDisabledLabel', { role: r.name }),
              disabled: !r.enabled,
            }))}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
