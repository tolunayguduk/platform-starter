import { useState } from 'react';
import { App, Form, Input, Modal } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { createAdminRole } from '../../../api/adminApi';

export function AddRoleModal({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  function handleClose() {
    form.resetFields();
    onClose();
  }

  async function handleSubmit() {
    if (!accessToken) return;
    const values = await form.validateFields();
    setSaving(true);
    try {
      await createAdminRole(accessToken, values.name.trim());
      message.success(t('admin.roleFunctions.newRole.success'));
      handleClose();
      onCreated();
    } catch (e) {
      message.error(
        e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.newRole.error')) : t('admin.roleFunctions.newRole.error'),
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      open={open}
      title={t('admin.roleFunctions.newRole.title')}
      onCancel={handleClose}
      onOk={handleSubmit}
      confirmLoading={saving}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="name" label={t('admin.roleFunctions.newRole.placeholder')} rules={[{ required: true, whitespace: true }]}>
          <Input placeholder={t('admin.roleFunctions.newRole.placeholder')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
