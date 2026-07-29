import { Typography, Divider, Form, Input, Button, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { useAuth } from '../../../auth/AuthContext.tsx';
import { changePassword, type ChangePasswordFields } from '../../../api/profile.ts';
import { ApiError } from '../../../api/client.ts';
import layoutStyles from './profileLayout.module.css';

export function ChangePasswordSection() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [form] = Form.useForm<ChangePasswordFields>();
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(values: ChangePasswordFields) {
    if (!accessToken) return;
    setSubmitting(true);
    try {
      await changePassword(accessToken, values);
      form.resetFields();
      message.success(t('profile.passwordSuccess'));
    } catch (e) {
      message.error(e instanceof ApiError ? e.body?.message ?? t('profile.passwordError') : t('profile.passwordError'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className={layoutStyles.fullWidth}>
      <Divider />
      <Typography.Title level={4}>{t('profile.passwordSection')}</Typography.Title>
      <Form form={form} layout="vertical" onFinish={handleSubmit} className={layoutStyles.fullWidth}>
        <Form.Item name="currentPassword" label={t('profile.currentPassword')} rules={[{ required: true }]}>
          <Input.Password size="large" />
        </Form.Item>
        <div className={layoutStyles.fieldRow}>
          <Form.Item
            name="newPassword"
            label={t('profile.newPassword')}
            rules={[{ required: true, min: 8 }]}
            className={layoutStyles.flexItem}
          >
            <Input.Password size="large" />
          </Form.Item>
          <Form.Item
            name="confirmNewPassword"
            label={t('profile.confirmNewPassword')}
            dependencies={['newPassword']}
            className={layoutStyles.flexItem}
            rules={[
              { required: true },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                  return Promise.reject(new Error());
                },
              }),
            ]}
          >
            <Input.Password size="large" />
          </Form.Item>
        </div>
        <Button type="primary" danger htmlType="submit" loading={submitting} size="large" block>
          {t('profile.changePassword')}
        </Button>
      </Form>
    </div>
  );
}