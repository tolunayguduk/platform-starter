import { useState } from 'react';
import { Alert, App, Button, Descriptions, Input, Modal, Space } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { inviteOrganizationMember, findUserByIdentifier } from '../../../api/adminApi';
import type { AdminUser } from '../../../types/admin';

interface InviteOrganizationMemberModalProps {
  open: boolean;
  organizationId: string;
  existingMemberIds: Set<string>;
  onClose: () => void;
  onInvited: () => void;
}

/** Exact username/email lookup only, never a browse/search - see AdminOrganizationService on the
 * backend for why (an organization admin must never be able to see the full user directory,
 * that would defeat the whole point of isolating organizations from each other). Sends a pending
 * invite - the target user must accept it themselves before they become a member. */
export function InviteOrganizationMemberModal({ open, organizationId, existingMemberIds, onClose, onInvited }: InviteOrganizationMemberModalProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [identifier, setIdentifier] = useState('');
  const [found, setFound] = useState<AdminUser | null>(null);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [inviting, setInviting] = useState(false);

  function handleClose() {
    setIdentifier('');
    setFound(null);
    setSearchError(null);
    onClose();
  }

  async function handleSearch() {
    if (!accessToken || !identifier.trim()) return;
    setSearching(true);
    setSearchError(null);
    setFound(null);
    try {
      const user = await findUserByIdentifier(accessToken, identifier.trim());
      setFound(user);
    } catch (e) {
      setSearchError(
        e instanceof ApiError ? (e.body?.message ?? t('admin.organizations.invite.notFound')) : t('admin.organizations.invite.notFound'),
      );
    } finally {
      setSearching(false);
    }
  }

  async function handleInvite() {
    if (!accessToken || !found) return;
    setInviting(true);
    try {
      await inviteOrganizationMember(accessToken, organizationId, found.id);
      message.success(t('admin.organizations.inviteSent'));
      handleClose();
      onInvited();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.organizations.error')) : t('admin.organizations.error'));
    } finally {
      setInviting(false);
    }
  }

  const alreadyMember = found !== null && existingMemberIds.has(found.id);

  return (
    <Modal open={open} title={t('admin.organizations.invite.title')} onCancel={handleClose} footer={null} destroyOnHidden>
      <Space.Compact style={{ width: '100%', marginBottom: 16 }}>
        <Input
          placeholder={t('admin.organizations.invite.placeholder')}
          value={identifier}
          onChange={(e) => setIdentifier(e.target.value)}
          onPressEnter={handleSearch}
        />
        <Button onClick={handleSearch} loading={searching}>
          {t('admin.organizations.invite.searchButton')}
        </Button>
      </Space.Compact>
      {searchError && <Alert type="error" message={searchError} style={{ marginBottom: 16 }} />}
      {found && (
        <>
          <Descriptions column={1} bordered size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label={t('admin.column.username')}>{found.username}</Descriptions.Item>
            <Descriptions.Item label={t('admin.column.email')}>{found.email}</Descriptions.Item>
          </Descriptions>
          <Button type="primary" block disabled={alreadyMember} loading={inviting} onClick={handleInvite}>
            {alreadyMember ? t('admin.organizations.invite.alreadyMember') : t('admin.organizations.invite.confirmButton')}
          </Button>
        </>
      )}
    </Modal>
  );
}
