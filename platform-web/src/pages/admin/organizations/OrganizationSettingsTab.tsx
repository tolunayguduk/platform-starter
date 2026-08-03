import { useState } from 'react';
import { App, Button, Input, Space, Switch, Typography } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { updateMembershipApproval } from '../../../api/adminApi';
import type { Organization } from '../../../types/admin';

export function OrganizationSettingsTab({ organization, onSettingsChanged }: { organization: Organization; onSettingsChanged: () => void }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [saving, setSaving] = useState(false);

  const inviteLink = `${window.location.origin}/register?joinOrganization=${organization.id}`;

  async function handleToggle(requiresApproval: boolean) {
    if (!accessToken) return;
    setSaving(true);
    try {
      await updateMembershipApproval(accessToken, organization.id, requiresApproval);
      message.success(t('admin.organizations.settings.updated'));
      onSettingsChanged();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.organizations.settings.updateError')) : t('admin.organizations.settings.updateError'));
    } finally {
      setSaving(false);
    }
  }

  async function handleCopyLink() {
    await navigator.clipboard.writeText(inviteLink);
    message.success(t('admin.organizations.settings.linkCopied'));
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <div>
        <Space align="center">
          <Switch checked={organization.membershipRequiresApproval} loading={saving} onChange={handleToggle} />
          <Typography.Text>{t('admin.organizations.settings.requiresApprovalLabel')}</Typography.Text>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0 }}>
          {t('admin.organizations.settings.requiresApprovalHint')}
        </Typography.Paragraph>
      </div>
      <div>
        <Typography.Text strong>{t('admin.organizations.settings.inviteLinkLabel')}</Typography.Text>
        <Typography.Paragraph type="secondary" style={{ marginTop: 4 }}>
          {t('admin.organizations.settings.inviteLinkHint')}
        </Typography.Paragraph>
        <Space.Compact style={{ width: '100%', maxWidth: 480 }}>
          <Input readOnly value={inviteLink} />
          <Button icon={<CopyOutlined />} onClick={handleCopyLink}>
            {t('admin.organizations.settings.copyLink')}
          </Button>
        </Space.Compact>
      </div>
    </Space>
  );
}
