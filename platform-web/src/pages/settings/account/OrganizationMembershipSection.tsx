import { useState } from 'react';
import { Typography, Divider, Space, Input, Button, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext.tsx';
import { joinOrganization } from '../../../api/organizationMembershipApi.ts';
import { ApiError } from '../../../api/client.ts';
import layoutStyles from './profileLayout.module.css';

/** Lets an already-registered plain user request to join an organization via its invite
 * link/code - subject to that organization's approval setting (see OrganizationSettingsTab on
 * the admin side). */
export function OrganizationMembershipSection() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [organizationId, setOrganizationId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleJoin() {
    if (!accessToken || !organizationId.trim()) return;
    setSubmitting(true);
    try {
      const result = await joinOrganization(accessToken, organizationId.trim());
      message.success(result.approved ? t('profile.organizationJoined') : t('profile.organizationJoinPending'));
      setOrganizationId('');
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('profile.organizationJoinError')) : t('profile.organizationJoinError'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className={layoutStyles.fullWidth}>
      <Divider />
      <Typography.Title level={4}>{t('profile.organizationSection')}</Typography.Title>
      <Typography.Paragraph type="secondary">{t('profile.organizationSectionHint')}</Typography.Paragraph>
      <Space.Compact style={{ width: '100%', maxWidth: 480 }}>
        <Input
          placeholder={t('profile.organizationIdPlaceholder')}
          value={organizationId}
          onChange={(e) => setOrganizationId(e.target.value)}
          onPressEnter={handleJoin}
        />
        <Button type="primary" loading={submitting} onClick={handleJoin}>
          {t('profile.organizationJoinButton')}
        </Button>
      </Space.Compact>
    </div>
  );
}
