import { useState } from 'react';
import { App, Button, Form, Input, Modal, Select, Space } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { createAdminTableRow } from '../../../api/adminApi';
import type { PermissionCatalogEntry } from '../../../types/admin';
import { ACCESS_LEVEL_OPTIONS, UI_POLICY_OPTIONS } from './constants';

interface AddFunctionModalProps {
  open: boolean;
  role: string;
  catalog: PermissionCatalogEntry[];
  alreadyGrantedPermissionIds: Set<number>;
  onClose: () => void;
  onCreated: () => void;
}

export function AddFunctionModal({ open, role, catalog, alreadyGrantedPermissionIds, onClose, onCreated }: AddFunctionModalProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [mode, setMode] = useState<'existing' | 'new'>('existing');
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  const availableCatalog = catalog.filter((p) => !alreadyGrantedPermissionIds.has(p.id) && p.enabled);

  function handleClose() {
    form.resetFields();
    setMode('existing');
    onClose();
  }

  async function handleSubmit() {
    if (!accessToken) return;
    const values = await form.validateFields();
    setSaving(true);
    try {
      let permissionId: number;
      if (mode === 'existing') {
        permissionId = values.permissionId;
      } else {
        const created = await createAdminTableRow(accessToken, 'PERMISSION', {
          key: values.key.trim(),
          ui_policy: values.uiPolicy,
          description: values.description?.trim() || null,
        });
        permissionId = Number(created.id);
      }
      await createAdminTableRow(accessToken, 'ROLE_PERMISSION', {
        role_name: role,
        permission_id: permissionId,
        access_level: values.accessLevel,
      });
      message.success(t('admin.roleFunctions.granted'));
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
      <Space style={{ marginBottom: 16 }}>
        <Button type={mode === 'existing' ? 'primary' : 'default'} onClick={() => setMode('existing')}>
          {t('admin.roleFunctions.newFunction.modeExisting')}
        </Button>
        <Button type={mode === 'new' ? 'primary' : 'default'} onClick={() => setMode('new')}>
          {t('admin.roleFunctions.newFunction.modeNew')}
        </Button>
      </Space>
      <Form form={form} layout="vertical" initialValues={{ accessLevel: 'GRANTED' }}>
        {mode === 'existing' ? (
          <Form.Item name="permissionId" label={t('admin.functionAccess.column.function')} rules={[{ required: true }]}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder={t('admin.roleFunctions.newFunction.keyPlaceholder')}
              options={availableCatalog.map((p) => ({ value: p.id, label: p.key }))}
            />
          </Form.Item>
        ) : (
          <>
            <Form.Item name="key" label={t('admin.roleFunctions.newFunction.keyPlaceholder')} rules={[{ required: true, whitespace: true }]}>
              <Input placeholder={t('admin.roleFunctions.newFunction.keyPlaceholder')} />
            </Form.Item>
            <Form.Item name="description" label={t('admin.roleFunctions.descriptionPlaceholder')}>
              <Input placeholder={t('admin.roleFunctions.descriptionPlaceholder')} />
            </Form.Item>
            <Form.Item name="uiPolicy" label={t('admin.roleFunctions.newFunction.uiPolicyPlaceholder')} rules={[{ required: true }]}>
              <Select options={UI_POLICY_OPTIONS.map((policy) => ({ value: policy, label: t(`admin.roleFunctions.uiPolicy.${policy}`) }))} />
            </Form.Item>
          </>
        )}
        <Form.Item name="accessLevel" label={t('admin.roleFunctions.column.accessLevel')} rules={[{ required: true }]}>
          <Select options={ACCESS_LEVEL_OPTIONS.map((level) => ({ value: level, label: t(`admin.roleFunctions.accessLevel.${level}`) }))} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
