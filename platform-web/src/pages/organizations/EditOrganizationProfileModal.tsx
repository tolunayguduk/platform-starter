import { useEffect, useState } from 'react';
import { App, Avatar, Form, Input, Modal } from 'antd';
import { TeamOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../store/AuthContext';
import { ApiError } from '../../api/client';
import { renameOrganization, updateOrganizationDescription, updateOrganizationImages } from '../../api/adminApi';
import type { OrganizationProfile } from '../../types/organization';
import styles from './OrganizationLandingPage.module.css';

interface FormValues {
  name: string;
  description: string;
  coverImageUrl: string;
  logoImageUrl: string;
}

export function EditOrganizationProfileModal({
  open,
  profile,
  onClose,
  onSaved,
}: {
  open: boolean;
  profile: OrganizationProfile;
  onClose: () => void;
  onSaved: () => void;
}) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [form] = Form.useForm<FormValues>();
  const [saving, setSaving] = useState(false);
  const coverPreview = Form.useWatch('coverImageUrl', form);
  const logoPreview = Form.useWatch('logoImageUrl', form);

  useEffect(() => {
    if (open) {
      form.setFieldsValue({
        name: profile.name,
        description: profile.description ?? '',
        coverImageUrl: profile.coverImageUrl ?? '',
        logoImageUrl: profile.logoImageUrl ?? '',
      });
    }
  }, [open, profile, form]);

  async function handleSubmit() {
    if (!accessToken) return;
    const values = await form.validateFields();
    setSaving(true);
    try {
      const tasks: Promise<void>[] = [];
      if (values.name.trim() !== profile.name) {
        tasks.push(renameOrganization(accessToken, profile.id, values.name.trim()));
      }
      if ((values.description ?? '') !== (profile.description ?? '')) {
        tasks.push(updateOrganizationDescription(accessToken, profile.id, values.description ?? ''));
      }
      if ((values.coverImageUrl ?? '') !== (profile.coverImageUrl ?? '') || (values.logoImageUrl ?? '') !== (profile.logoImageUrl ?? '')) {
        tasks.push(
          updateOrganizationImages(accessToken, profile.id, {
            coverImageUrl: values.coverImageUrl || null,
            logoImageUrl: values.logoImageUrl || null,
          }),
        );
      }
      await Promise.all(tasks);
      message.success(t('organization.editSuccess'));
      onSaved();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('organization.editError')) : t('organization.editError'));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      open={open}
      title={t('organization.editProfile')}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={saving}
      okText={t('profile.save')}
      destroyOnHidden
    >
      <div className={styles.previewRow}>
        <div className={styles.previewCover} style={coverPreview ? { backgroundImage: `url(${coverPreview})` } : undefined} />
        <Avatar size={64} shape="square" src={logoPreview || undefined} icon={<TeamOutlined />} />
      </div>
      <Form form={form} layout="vertical">
        <Form.Item name="name" label={t('organization.nameLabel')} rules={[{ required: true, whitespace: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="coverImageUrl" label={t('organization.coverImageLabel')}>
          <Input placeholder="https://..." />
        </Form.Item>
        <Form.Item name="logoImageUrl" label={t('organization.logoImageLabel')}>
          <Input placeholder="https://..." />
        </Form.Item>
        <Form.Item name="description" label={t('organization.aboutLabel')}>
          <Input.TextArea rows={4} placeholder={t('organization.aboutPlaceholder')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
