import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Avatar, Button, List, Result, Spin, Typography } from 'antd';
import { UserOutlined, EditOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../store/AuthContext';
import { fetchUserProfile } from '../../api/userDirectoryApi';
import type { UserProfileSummary } from '../../types/user';
import { OrganizationNameLink } from '../../components/OrganizationNameLink';
import styles from './UserProfilePage.module.css';

export function UserProfilePage() {
  const { id } = useParams<{ id: string }>();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { accessToken, user: currentUser } = useAuth();
  const [profile, setProfile] = useState<UserProfileSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!accessToken || !id) return;
    setLoading(true);
    setNotFound(false);
    fetchUserProfile(accessToken, id)
      .then(setProfile)
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false));
  }, [accessToken, id]);

  if (loading) {
    return (
      <div className={styles.centered}>
        <Spin size="large" />
      </div>
    );
  }

  if (notFound || !profile) {
    return (
      <Result
        status="404"
        title={t('userProfile.notFoundTitle')}
        subTitle={t('userProfile.notFoundSubtitle')}
        extra={
          <Button type="primary" onClick={() => navigate('/')}>
            {t('notFound.backHome')}
          </Button>
        }
      />
    );
  }

  const isSelf = profile.username === currentUser?.username;

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <Avatar size={112} src={profile.avatarUrl || undefined} icon={<UserOutlined />} className={styles.avatar} />
        <div className={styles.identity}>
          <Typography.Title level={3} className={styles.name}>
            {profile.fullName || profile.username}
          </Typography.Title>
          <Typography.Text type="secondary">@{profile.username}</Typography.Text>
        </div>
        {isSelf && (
          <Button icon={<EditOutlined />} className={styles.editButton} onClick={() => navigate('/settings?tab=account')}>
            {t('userProfile.editInSettings')}
          </Button>
        )}
      </div>
      <div className={styles.body}>
        <Typography.Title level={5} className={styles.sectionTitle}>
          {t('userProfile.organizations')}
        </Typography.Title>
        {profile.organizations.length === 0 ? (
          <Typography.Paragraph type="secondary">{t('userProfile.noOrganizations')}</Typography.Paragraph>
        ) : (
          <List
            dataSource={profile.organizations}
            renderItem={(org) => (
              <List.Item extra={<Typography.Text type="secondary">{t('organization.memberCount', { count: org.memberCount })}</Typography.Text>}>
                <OrganizationNameLink id={org.id} name={org.name} logoImageUrl={org.logoImageUrl} />
              </List.Item>
            )}
          />
        )}
      </div>
    </div>
  );
}
