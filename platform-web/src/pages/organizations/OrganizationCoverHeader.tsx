import { useState } from 'react';
import { Avatar, Button, Typography } from 'antd';
import { EditOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useThemeSettings } from '../../store/ThemeSettingsContext';
import { useEffectiveDarkMode } from '../../hooks/useEffectiveDarkMode';
import { getEffectiveAccentColor } from '../../utils/contrast';
import type { OrganizationProfile } from '../../types/organization';
import { OrganizationJoinButton } from './OrganizationJoinButton';
import { EditOrganizationProfileModal } from './EditOrganizationProfileModal';
import { OrganizationMembersModal } from './OrganizationMembersModal';
import styles from './OrganizationLandingPage.module.css';

export function OrganizationCoverHeader({ profile, onChanged }: { profile: OrganizationProfile; onChanged: () => void }) {
  const { t } = useTranslation();
  const { themeColor } = useThemeSettings();
  const dark = useEffectiveDarkMode(themeColor);
  const accentColor = getEffectiveAccentColor(themeColor, dark);
  const [editOpen, setEditOpen] = useState(false);
  const [membersOpen, setMembersOpen] = useState(false);

  const coverStyle = profile.coverImageUrl
    ? { backgroundImage: `url(${profile.coverImageUrl})` }
    : { background: `linear-gradient(135deg, ${accentColor}, color-mix(in srgb, ${accentColor} 25%, transparent))` };

  return (
    <div className={styles.header}>
      <div className={styles.cover} style={coverStyle} />
      <div className={styles.headerBar}>
        <Avatar size={112} shape="square" src={profile.logoImageUrl || undefined} icon={<TeamOutlined />} className={styles.avatar} />
        <div className={styles.headerActions}>
          {profile.canEdit ? (
            <Button icon={<EditOutlined />} onClick={() => setEditOpen(true)}>
              {t('organization.editProfile')}
            </Button>
          ) : (
            <OrganizationJoinButton profile={profile} onChanged={onChanged} />
          )}
        </div>
      </div>
      <div className={styles.identity}>
        <Typography.Title level={3} className={styles.name}>
          {profile.name}
        </Typography.Title>
        <Typography.Link onClick={() => setMembersOpen(true)} className={styles.memberCount}>
          <UserOutlined /> {t('organization.memberCount', { count: profile.memberCount })}
        </Typography.Link>
      </div>
      <EditOrganizationProfileModal
        open={editOpen}
        profile={profile}
        onClose={() => setEditOpen(false)}
        onSaved={() => {
          setEditOpen(false);
          onChanged();
        }}
      />
      <OrganizationMembersModal open={membersOpen} organizationId={profile.id} onClose={() => setMembersOpen(false)} />
    </div>
  );
}
