import { useState } from 'react';
import { App, Form, Input, Modal } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { createOrganization } from '../../../api/adminApi';

export function AddOrganizationModal({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
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
    setSaving(true);
    try {
      await createOrganization(accessToken, name.trim());
      message.success(t('admin.organizations.newOrganization.success'));
      handleClose();
      onCreated();
    } catch (e) {
      message.error(
        e instanceof ApiError
          ? (e.body?.message ?? t('admin.organizations.newOrganization.error'))
          : t('admin.organizations.newOrganization.error'),
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      open={open}
      title={t('admin.organizations.newOrganization.title')}
      onCancel={handleClose}
      onOk={handleSubmit}
      confirmLoading={saving}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="name" label={t('admin.organizations.newOrganization.placeholder')} rules={[{ required: true, whitespace: true }]}>
          <Input placeholder={t('admin.organizations.newOrganization.placeholder')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
