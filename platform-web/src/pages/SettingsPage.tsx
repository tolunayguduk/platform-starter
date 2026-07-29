import type { ReactNode } from 'react';
import { Typography, Tabs, type TabsProps } from 'antd';
import {
  BgColorsOutlined,
  GlobalOutlined,
  UserOutlined,
  BellOutlined,
  SafetyOutlined,
  CreditCardOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { ThemeSettingsTab } from './settings/ThemeSettingsTab';
import { LanguageSettingsTab } from './settings/LanguageSettingsTab';
import { AccountTab } from './settings/AccountTab';
import { ComingSoonTab } from './settings/ComingSoonTab';
import styles from './SettingsPage.module.css';

const TAB_KEYS = ['theme', 'language', 'account', 'notifications', 'security', 'billing'];

function CategoryLabel({ icon, text }: { icon: ReactNode; text: string }) {
  return (
    <span>
      {icon} {text}
    </span>
  );
}

export function SettingsPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();

  const requestedTab = searchParams.get('tab') ?? '';
  const activeKey = TAB_KEYS.includes(requestedTab) ? requestedTab : 'account';

  const items: TabsProps['items'] = [
    {
      key: 'account',
      label: <CategoryLabel icon={<UserOutlined />} text={t('settings.categories.account')} />,
      children: <AccountTab />,
    },
    {
      key: 'theme',
      label: <CategoryLabel icon={<BgColorsOutlined />} text={t('settings.categories.theme')} />,
      children: <ThemeSettingsTab />,
    },
    {
      key: 'language',
      label: <CategoryLabel icon={<GlobalOutlined />} text={t('settings.categories.language')} />,
      children: <LanguageSettingsTab />,
    },
    {
      key: 'notifications',
      label: <CategoryLabel icon={<BellOutlined />} text={t('settings.categories.notifications')} />,
      children: <ComingSoonTab titleKey="settings.categories.notifications" />,
    },
    {
      key: 'security',
      label: <CategoryLabel icon={<SafetyOutlined />} text={t('settings.categories.security')} />,
      children: <ComingSoonTab titleKey="settings.categories.security" />,
    },
    {
      key: 'billing',
      label: <CategoryLabel icon={<CreditCardOutlined />} text={t('settings.categories.billing')} />,
      children: <ComingSoonTab titleKey="settings.categories.billing" />,
    },
  ];

  return (
    <>
      <Typography.Title level={3}>{t('settings.title')}</Typography.Title>
      <Tabs
        tabPlacement="start"
        items={items}
        activeKey={activeKey}
        onChange={(key) => setSearchParams({ tab: key })}
        className={styles.tabs}
      />
    </>
  );
}