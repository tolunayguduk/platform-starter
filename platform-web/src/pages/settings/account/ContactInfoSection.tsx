import { Typography, Divider, Form, Input } from 'antd';
import { useTranslation } from 'react-i18next';
import layoutStyles from './profileLayout.module.css';

export function ContactInfoSection() {
  const { t } = useTranslation();

  return (
    <>
      <Divider />
      <Typography.Title level={4}>{t('profile.contactSection')}</Typography.Title>

      <div className={layoutStyles.fieldRow}>
        <Form.Item name="phoneNumber" label={t('profile.phoneNumber')} className={layoutStyles.flexItem}>
          <Input size="large" />
        </Form.Item>
        <Form.Item
          name="alternateEmail"
          label={t('profile.alternateEmail')}
          rules={[{ type: 'email' }]}
          className={layoutStyles.flexItem}
        >
          <Input size="large" />
        </Form.Item>
      </div>
      <Form.Item name="addressLine" label={t('profile.addressLine')}>
        <Input size="large" />
      </Form.Item>
      <div className={layoutStyles.fieldRow}>
        <Form.Item name="city" label={t('profile.city')} className={layoutStyles.flexItem}>
          <Input size="large" />
        </Form.Item>
        <Form.Item name="country" label={t('profile.country')} className={layoutStyles.flexItem}>
          <Input size="large" />
        </Form.Item>
      </div>
    </>
  );
}