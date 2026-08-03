import type { CSSProperties } from 'react';
import { Layout, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { Link, Outlet } from 'react-router-dom';
import { useThemeSettings } from '../store/ThemeSettingsContext';
import { getContrastOnColor } from '../utils/contrast';
import { UserMenu } from './UserMenu';
import { OrganizationSearchBox } from './OrganizationSearchBox';
import styles from './AppLayout.module.css';

const { Header, Content } = Layout;

export function AppLayout() {
  const { t } = useTranslation();
  const { themeColor } = useThemeSettings();
  const { text: navText, overlay: navOverlay } = getContrastOnColor(themeColor);

  const headerVars = { '--nav-bg': themeColor, '--nav-text': navText } as CSSProperties;
  const contentVars = { '--content-bg': `color-mix(in srgb, ${themeColor} 6%, var(--bg))` } as CSSProperties;

  return (
    <Layout className={styles.layout}>
      <Header className={styles.header} style={headerVars}>
        <Link to="/" className={styles.appName}>
          <Typography.Text strong className={styles.appNameText}>
            {t('nav.appName')}
          </Typography.Text>
        </Link>
        <OrganizationSearchBox />
        <UserMenu textColor={navText} overlayColor={navOverlay} />
      </Header>
      <Content className={styles.content} style={contentVars}>
        <div className={styles.contentInner}>
          <Outlet />
        </div>
      </Content>
    </Layout>
  );
}