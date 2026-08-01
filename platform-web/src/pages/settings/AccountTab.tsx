import { useEffect, useState } from 'react';
import { Typography, Form, Button, App } from 'antd';
import { useTranslation } from 'react-i18next';
import dayjs, { type Dayjs } from 'dayjs';
import { useAuth } from '../../store/AuthContext';
import { fetchMyProfile, updateMyProfile } from '../../api/profileApi';
import type { MyProfile, UpdateProfileFields } from '../../types/profile';
import { ApiError } from '../../api/client';
import { AvatarNameFields } from './account/AvatarNameFields';
import { PersonalInfoSection } from './account/PersonalInfoSection';
import { ContactInfoSection } from './account/ContactInfoSection';
import { ChangePasswordSection } from './account/ChangePasswordSection';
import { ConsentHistorySection } from './account/ConsentHistorySection';
import layoutStyles from './account/profileLayout.module.css';

type ProfileFormValues = Omit<UpdateProfileFields, 'birthDate'> & { birthDate: Dayjs | null };

export function AccountTab() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [form] = Form.useForm<ProfileFormValues>();
  const [profile, setProfile] = useState<MyProfile | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!accessToken) return;
    fetchMyProfile(accessToken).then((data) => {
      setProfile(data);
      form.setFieldsValue({ ...data, birthDate: data.birthDate ? dayjs(data.birthDate) : null });
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken]);

  async function handleSubmit(values: ProfileFormValues) {
    if (!accessToken) return;
    setSubmitting(true);
    try {
      const updated = await updateMyProfile(accessToken, {
        ...values,
        birthDate: values.birthDate ? values.birthDate.format('YYYY-MM-DD') : null,
      });
      setProfile(updated);
      message.success(t('profile.saveSuccess'));
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('profile.saveError')) : t('profile.saveError'));
    } finally {
      setSubmitting(false);
    }
  }

  if (!profile) {
    return null;
  }

  return (
    <div className={layoutStyles.fullWidth}>
      <Typography.Title level={4}>{t('settings.categories.account')}</Typography.Title>

      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <AvatarNameFields />
        <PersonalInfoSection username={profile.username} />
        <ContactInfoSection />
        <Button type="primary" htmlType="submit" loading={submitting} size="large" block className={layoutStyles.mt8}>
          {t('profile.save')}
        </Button>
      </Form>

      <ChangePasswordSection />
      <ConsentHistorySection consents={profile.consents} />
    </div>
  );
}