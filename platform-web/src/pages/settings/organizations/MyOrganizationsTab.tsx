import { useEffect, useState } from 'react';
import { Avatar, Card, Col, Empty, Row, Typography } from 'antd';
import { TeamOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../store/AuthContext';
import { useThemeSettings } from '../../../store/ThemeSettingsContext';
import { useEffectiveDarkMode } from '../../../hooks/useEffectiveDarkMode';
import { getEffectiveAccentColor } from '../../../utils/contrast';
import { fetchMyOrganizations } from '../../../api/organizationMembershipApi';
import type { OrganizationSearchResult } from '../../../types/organization';
import styles from './MyOrganizationsTab.module.css';

export function MyOrganizationsTab() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { accessToken } = useAuth();
  const { themeColor } = useThemeSettings();
  const dark = useEffectiveDarkMode(themeColor);
  const accentColor = getEffectiveAccentColor(themeColor, dark);
  const [organizations, setOrganizations] = useState<OrganizationSearchResult[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!accessToken) return;
    setLoading(true);
    fetchMyOrganizations(accessToken)
      .then(setOrganizations)
      .finally(() => setLoading(false));
  }, [accessToken]);

  return (
    <>
      <Typography.Title level={4}>{t('settings.categories.organizations')}</Typography.Title>
      <Typography.Paragraph type="secondary">{t('myOrganizations.hint')}</Typography.Paragraph>
      {!loading && organizations.length === 0 ? (
        <Empty description={t('myOrganizations.empty')} />
      ) : (
        <Row gutter={[16, 16]}>
          {organizations.map((org) => (
            <Col key={org.id} xs={24} sm={12} lg={8}>
              <Card
                hoverable
                loading={loading}
                className={styles.card}
                onClick={() => navigate(`/organizations/${org.id}`)}
                cover={
                  <div
                    className={styles.cover}
                    style={
                      org.coverImageUrl
                        ? { backgroundImage: `url(${org.coverImageUrl})` }
                        : { background: `linear-gradient(135deg, ${accentColor}, color-mix(in srgb, ${accentColor} 25%, transparent))` }
                    }
                  >
                    <Avatar size={56} shape="square" src={org.logoImageUrl || undefined} icon={<TeamOutlined />} className={styles.avatar} />
                  </div>
                }
              >
                <Card.Meta title={org.name} description={t('organization.memberCount', { count: org.memberCount })} />
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </>
  );
}
