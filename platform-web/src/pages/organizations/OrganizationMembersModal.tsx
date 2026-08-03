import { useEffect, useState } from 'react';
import { Avatar, List, Modal } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../store/AuthContext';
import { fetchOrganizationMembers } from '../../api/organizationDirectoryApi';
import type { OrganizationMemberSummary } from '../../types/organization';

export function OrganizationMembersModal({
  open,
  organizationId,
  onClose,
}: {
  open: boolean;
  organizationId: string;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const [members, setMembers] = useState<OrganizationMemberSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!open || !accessToken) return;
    setLoading(true);
    fetchOrganizationMembers(accessToken, organizationId)
      .then(setMembers)
      .finally(() => setLoading(false));
  }, [open, accessToken, organizationId]);

  return (
    <Modal open={open} title={t('organization.membersModalTitle')} onCancel={onClose} footer={null} destroyOnHidden>
      <List
        loading={loading}
        dataSource={members}
        renderItem={(member) => (
          <List.Item>
            <List.Item.Meta
              avatar={<Avatar icon={<UserOutlined />} />}
              title={member.fullName || member.username}
              description={member.fullName ? member.username : undefined}
            />
          </List.Item>
        )}
      />
    </Modal>
  );
}
