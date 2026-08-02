import { useState } from 'react';
import { App, Button, Descriptions, Form, Input, Modal, Select, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { updateUserIdentity, updateUserRoles } from '../../../api/adminApi';
import type { AdminRole, AdminUser } from '../../../types/admin';
import { buildRoleSelectOptions } from './roleSelectOptions';

function sameRoleSet(a: string[], b: string[]): boolean {
  if (a.length !== b.length) return false;
  const sorted = [...b].sort();
  return [...a].sort().every((role, i) => role === sorted[i]);
}

interface EditUserModalProps {
  open: boolean;
  user: AdminUser;
  isSelf: boolean;
  availableRoles: AdminRole[];
  callerIsPlatformScoped: boolean;
  onClose: () => void;
  onSaved: () => void;
}

export function EditUserModal({ open, user, isSelf, availableRoles, callerIsPlatformScoped, onClose, onSaved }: EditUserModalProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [form] = Form.useForm();
  const [mode, setMode] = useState<'edit' | 'confirm'>('edit');
  const [pendingChanges, setPendingChanges] = useState<{ username?: string; email?: string; roles?: string[] }>({});
  const [saving, setSaving] = useState(false);

  function handleClose() {
    setMode('edit');
    setPendingChanges({});
    onClose();
  }

  function handleReview() {
    const values = form.getFieldsValue();
    const changes: { username?: string; email?: string; roles?: string[] } = {};
    if (values.username !== user.username) changes.username = values.username;
    if (values.email !== user.email) changes.email = values.email;
    if (!sameRoleSet(values.roles ?? [], user.roles)) changes.roles = values.roles;
    setPendingChanges(changes);
    setMode('confirm');
  }

  async function handleConfirm() {
    if (!accessToken) return;
    setSaving(true);
    try {
      if (pendingChanges.username !== undefined || pendingChanges.email !== undefined) {
        await updateUserIdentity(accessToken, user.id, {
          username: pendingChanges.username ?? user.username,
          email: pendingChanges.email ?? user.email,
        });
      }
      if (pendingChanges.roles !== undefined) {
        await updateUserRoles(accessToken, user.id, pendingChanges.roles);
      }
      message.success(t('admin.editUser.success'));
      onSaved();
      handleClose();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.editUser.error')) : t('admin.editUser.error'));
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
      title={mode === 'edit' ? t('admin.editUser.title') : t('admin.editUser.confirmTitle')}
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
          initialValues={{ username: user.username, email: user.email, roles: user.roles }}
        >
          <Form.Item name="username" label={t('admin.editUser.fields.username')}>
            <Input />
          </Form.Item>
          <Form.Item name="email" label={t('admin.editUser.fields.email')}>
            <Input />
          </Form.Item>
          <Form.Item
            name="roles"
            label={t('admin.editUser.fields.roles')}
            extra={isSelf ? t('admin.editUser.selfRoleHint') : undefined}
          >
            <Select mode="multiple" options={buildRoleSelectOptions(availableRoles, callerIsPlatformScoped, t)} />
          </Form.Item>
        </Form>
      ) : hasChanges ? (
        <Descriptions column={1} bordered size="small">
          {pendingChanges.username !== undefined && (
            <Descriptions.Item label={t('admin.editUser.fields.username')}>
              <span style={{ textDecoration: 'line-through', opacity: 0.6 }}>{user.username}</span>
              {' → '}
              <strong>{pendingChanges.username}</strong>
            </Descriptions.Item>
          )}
          {pendingChanges.email !== undefined && (
            <Descriptions.Item label={t('admin.editUser.fields.email')}>
              <span style={{ textDecoration: 'line-through', opacity: 0.6 }}>{user.email}</span>
              {' → '}
              <strong>{pendingChanges.email}</strong>
            </Descriptions.Item>
          )}
          {pendingChanges.roles !== undefined && (
            <Descriptions.Item label={t('admin.editUser.fields.roles')}>
              <span style={{ textDecoration: 'line-through', opacity: 0.6 }}>{user.roles.join(', ') || '-'}</span>
              {' → '}
              <strong>{pendingChanges.roles.join(', ') || '-'}</strong>
            </Descriptions.Item>
          )}
        </Descriptions>
      ) : (
        <Typography.Text type="secondary">{t('admin.editRow.noChanges')}</Typography.Text>
      )}
    </Modal>
  );
}
