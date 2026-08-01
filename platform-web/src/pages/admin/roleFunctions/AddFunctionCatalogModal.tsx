import { useState } from 'react';
import { App, Form, Input, Modal, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { createAdminTableRow } from '../../../api/adminApi';
import { UI_POLICY_OPTIONS } from './constants';

export function AddFunctionCatalogModal({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
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
      await createAdminTableRow(accessToken, 'PERMISSION', {
        key: values.key.trim(),
        ui_policy: values.uiPolicy,
        description: values.description?.trim() || null,
      });
      message.success(t('admin.roleFunctions.newFunction.success'));
      handleClose();
      onCreated();
    } catch (e) {
      message.error(
        e instanceof ApiError
          ? (e.body?.message ?? t('admin.roleFunctions.newFunction.error'))
          : t('admin.roleFunctions.newFunction.error'),
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      open={open}
      title={t('admin.roleFunctions.newFunction.title')}
      onCancel={handleClose}
      onOk={handleSubmit}
      confirmLoading={saving}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="key" label={t('admin.roleFunctions.newFunction.keyPlaceholder')} rules={[{ required: true, whitespace: true }]}>
          <Input placeholder={t('admin.roleFunctions.newFunction.keyPlaceholder')} />
        </Form.Item>
        <Form.Item name="description" label={t('admin.roleFunctions.descriptionPlaceholder')}>
          <Input placeholder={t('admin.roleFunctions.descriptionPlaceholder')} />
        </Form.Item>
        <Form.Item
          name="uiPolicy"
          label={t('admin.roleFunctions.newFunction.uiPolicyPlaceholder')}
          rules={[{ required: true }]}
          initialValue="HIDE_IF_DENIED"
        >
          <Select options={UI_POLICY_OPTIONS.map((policy) => ({ value: policy, label: t(`admin.roleFunctions.uiPolicy.${policy}`) }))} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
