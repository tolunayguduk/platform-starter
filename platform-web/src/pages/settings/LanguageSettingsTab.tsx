import { Typography, Radio } from 'antd';
import { useTranslation } from 'react-i18next';

export function LanguageSettingsTab() {
  const { t, i18n } = useTranslation();

  return (
    <>
      <Typography.Title level={4}>{t('settings.categories.language')}</Typography.Title>
      <Typography.Paragraph type="secondary">{t('settings.language.description')}</Typography.Paragraph>
      <Radio.Group
        value={i18n.resolvedLanguage ?? i18n.language}
        onChange={(e) => i18n.changeLanguage(e.target.value)}
        optionType="button"
        options={[
          { label: t('settings.language.tr'), value: 'tr' },
          { label: t('settings.language.en'), value: 'en' },
        ]}
      />
    </>
  );
}