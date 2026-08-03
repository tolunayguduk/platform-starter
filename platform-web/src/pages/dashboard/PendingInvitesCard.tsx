import { useEffect, useState } from 'react';
import { App, Button, Card, List, Space } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../store/AuthContext';
import { ApiError } from '../../api/client';
import { acceptOrganizationInvite, declineOrganizationInvite, fetchMyOrganizationInvites } from '../../api/organizationMembershipApi';
import type { OrganizationMembershipRequest } from '../../types/admin';

/** Rendered only when the caller has at least one pending organization invite - a manager
 * inviting a user never grants membership immediately, the user must accept it here first. */
export function PendingInvitesCard() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [invites, setInvites] = useState<OrganizationMembershipRequest[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [resolvingId, setResolvingId] = useState<number | null>(null);

  function loadInvites() {
    if (!accessToken) return;
    fetchMyOrganizationInvites(accessToken)
      .then(setInvites)
      .finally(() => setLoaded(true));
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadInvites, [accessToken]);

  async function handleAccept(invite: OrganizationMembershipRequest) {
    if (!accessToken) return;
    setResolvingId(invite.id);
    try {
      await acceptOrganizationInvite(accessToken, invite.id);
      message.success(t('dashboard.invites.accepted', { organization: invite.organizationName }));
      loadInvites();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('dashboard.invites.error')) : t('dashboard.invites.error'));
    } finally {
      setResolvingId(null);
    }
  }

  async function handleDecline(invite: OrganizationMembershipRequest) {
    if (!accessToken) return;
    setResolvingId(invite.id);
    try {
      await declineOrganizationInvite(accessToken, invite.id);
      loadInvites();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('dashboard.invites.error')) : t('dashboard.invites.error'));
    } finally {
      setResolvingId(null);
    }
  }

  if (!loaded || invites.length === 0) {
    return null;
  }

  return (
    <Card title={t('dashboard.invites.title')} size="small">
      <List
        dataSource={invites}
        renderItem={(invite) => (
          <List.Item
            actions={[
              <Button key="accept" type="primary" size="small" loading={resolvingId === invite.id} onClick={() => handleAccept(invite)}>
                {t('dashboard.invites.accept')}
              </Button>,
              <Button key="decline" size="small" loading={resolvingId === invite.id} onClick={() => handleDecline(invite)}>
                {t('dashboard.invites.decline')}
              </Button>,
            ]}
          >
            <Space>{t('dashboard.invites.item', { organization: invite.organizationName })}</Space>
          </List.Item>
        )}
      />
    </Card>
  );
}
