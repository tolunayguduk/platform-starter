import { useState } from 'react';
import { App, Button, Popconfirm } from 'antd';
import { ClockCircleOutlined, LogoutOutlined, UsergroupAddOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../store/AuthContext';
import { ApiError } from '../../api/client';
import { joinOrganization, leaveOrganization } from '../../api/organizationMembershipApi';
import type { OrganizationProfile } from '../../types/organization';

/** The membership action on an organization's landing page - a single button that toggles
 * between "Join" and "Leave" depending on the caller's current membership, plus a disabled
 * "Pending" state while a self-service join request awaits the manager's approval. See
 * AccountTab, which no longer has its own join form now that this button exists here instead. */
export function OrganizationJoinButton({ profile, onChanged }: { profile: OrganizationProfile; onChanged: () => void }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [submitting, setSubmitting] = useState(false);

  if (profile.hasPendingJoinRequest) {
    return (
      <Button disabled icon={<ClockCircleOutlined />}>
        {t('organization.pending')}
      </Button>
    );
  }

  async function handleJoin() {
    if (!accessToken) return;
    setSubmitting(true);
    try {
      const result = await joinOrganization(accessToken, profile.id);
      message.success(result.approved ? t('organization.joined') : t('organization.joinRequested'));
      onChanged();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('organization.joinError')) : t('organization.joinError'));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleLeave() {
    if (!accessToken) return;
    setSubmitting(true);
    try {
      await leaveOrganization(accessToken, profile.id);
      message.success(t('organization.left'));
      onChanged();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('organization.leaveError')) : t('organization.leaveError'));
    } finally {
      setSubmitting(false);
    }
  }

  if (profile.isMember) {
    return (
      <Popconfirm title={t('organization.confirmLeave')} okButtonProps={{ danger: true }} onConfirm={handleLeave}>
        <Button danger icon={<LogoutOutlined />} loading={submitting}>
          {t('organization.leave')}
        </Button>
      </Popconfirm>
    );
  }

  return (
    <Button type="primary" icon={<UsergroupAddOutlined />} loading={submitting} onClick={handleJoin}>
      {t('organization.join')}
    </Button>
  );
}
