import type { CSSProperties } from 'react';
import { Dropdown, Space, Avatar, Typography, type MenuProps } from 'antd';
import { UserOutlined, DownOutlined, SettingOutlined, LogoutOutlined, DashboardOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../store/AuthContext';
import { useAdminAccessScope } from '../hooks/useAdminAccessScope';
import styles from './UserMenu.module.css';

/** The navbar's hover dropdown: username -> profile / settings / logout.
 * textColor/overlayColor come from AppLayout (contrast against the chosen theme color). */
export function UserMenu({ textColor, overlayColor }: { textColor: string; overlayColor: string }) {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { scope } = useAdminAccessScope();

  const isAdmin = (scope?.platformScoped || scope?.organizationScoped) ?? false;

  const menuItems: MenuProps['items'] = [
    { key: 'settings', icon: <SettingOutlined />, label: t('nav.settings') },
    ...(isAdmin ? [{ key: 'admin', icon: <DashboardOutlined />, label: t('nav.admin') }] : []),
    { type: 'divider' as const },
    { key: 'logout', icon: <LogoutOutlined />, label: t('nav.logout'), danger: true },
  ];

  async function handleMenuClick({ key }: { key: string }) {
    if (key === 'profile') navigate('/settings?tab=account');
    else if (key === 'settings') navigate('/settings');
    else if (key === 'admin') navigate('/admin');
    else if (key === 'logout') {
      await logout();
      navigate('/login', { replace: true });
    }
  }

  const vars = { '--nav-text': textColor, '--nav-overlay': overlayColor } as CSSProperties;

  return (
    <Dropdown menu={{ items: menuItems, onClick: handleMenuClick }} trigger={['hover']}>
      <Space className={styles.trigger} style={vars} size={8}>
        <Avatar size="small" icon={<UserOutlined />} className={styles.avatar} />
        <Typography.Text className={styles.username}>{user?.username}</Typography.Text>
        <DownOutlined className={styles.chevron} />
      </Space>
    </Dropdown>
  );
}