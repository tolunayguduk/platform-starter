import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Button, Result, Spin, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../store/AuthContext';
import { fetchOrganizationProfile } from '../../api/organizationDirectoryApi';
import type { OrganizationProfile } from '../../types/organization';
import { OrganizationCoverHeader } from './OrganizationCoverHeader';
import styles from './OrganizationLandingPage.module.css';

export function OrganizationLandingPage() {
  const { id } = useParams<{ id: string }>();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { accessToken } = useAuth();
  const [profile, setProfile] = useState<OrganizationProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  function loadProfile() {
    if (!accessToken || !id) return;
    setLoading(true);
    setNotFound(false);
    fetchOrganizationProfile(accessToken, id)
      .then(setProfile)
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false));
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadProfile, [accessToken, id]);

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
        title={t('organization.notFoundTitle')}
        subTitle={t('organization.notFoundSubtitle')}
        extra={
          <Button type="primary" onClick={() => navigate('/')}>
            {t('notFound.backHome')}
          </Button>
        }
      />
    );
  }

  return (
    <div className={styles.page}>
      <OrganizationCoverHeader profile={profile} onChanged={loadProfile} />
      <div className={styles.body}>
        <Typography.Title level={5} className={styles.aboutTitle}>
          {t('organization.about')}
        </Typography.Title>
        <Typography.Paragraph className={styles.aboutText} type={profile.description ? undefined : 'secondary'}>
          {profile.description || t('organization.noAbout')}
        </Typography.Paragraph>
      </div>
    </div>
  );
}
