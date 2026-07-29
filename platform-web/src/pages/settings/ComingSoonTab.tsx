import { Typography } from 'antd';
import { useTranslation } from 'react-i18next';

export function ComingSoonTab({ titleKey }: { titleKey: string }) {
  const { t } = useTranslation();

  return (
    <>
      <Typography.Title level={4}>{t(titleKey)}</Typography.Title>
      <Typography.Text type="secondary">{t('settings.comingSoon')}</Typography.Text>
    </>
  );
}