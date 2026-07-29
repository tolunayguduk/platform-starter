import { Typography, Divider, Form, Input, Select, DatePicker } from 'antd';
import { useTranslation } from 'react-i18next';
import layoutStyles from './profileLayout.module.css';

export function PersonalInfoSection({ username }: { username: string }) {
  const { t } = useTranslation();

  return (
    <>
      <Divider />
      <Typography.Title level={4}>{t('profile.identitySection')}</Typography.Title>

      <Form.Item label={t('profile.username')}>
        <Input size="large" value={username} disabled />
      </Form.Item>
      <Form.Item name="email" label={t('profile.email')} rules={[{ required: true, type: 'email' }]}>
        <Input size="large" />
      </Form.Item>
      <div className={layoutStyles.fieldRow}>
        <Form.Item name="birthDate" label={t('profile.birthDate')} className={layoutStyles.flexItem}>
          <DatePicker size="large" className={layoutStyles.fullWidth} />
        </Form.Item>
        <Form.Item name="locale" label={t('profile.locale')} className={layoutStyles.flexItem}>
          <Select
            size="large"
            allowClear
            options={[
              { value: 'tr', label: t('settings.language.tr') },
              { value: 'en', label: t('settings.language.en') },
            ]}
          />
        </Form.Item>
      </div>
    </>
  );
}