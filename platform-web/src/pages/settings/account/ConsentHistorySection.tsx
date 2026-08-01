import { Typography, Divider, Table } from 'antd';
import { useTranslation } from 'react-i18next';
import type { Consent } from '../../../types/profile.ts';

export function ConsentHistorySection({ consents }: { consents: Consent[] }) {
  const { t } = useTranslation();

  return (
    <>
      <Divider />
      <Typography.Title level={4}>{t('profile.consentSection')}</Typography.Title>
      <Table
        size="small"
        rowKey={(row) => `${row.consentType}-${row.grantedAt}`}
        dataSource={consents}
        pagination={false}
        locale={{ emptyText: t('profile.noConsents') }}
        columns={[
          { title: t('profile.consentType'), dataIndex: 'consentType' },
          {
            title: t('profile.grantedAt'),
            dataIndex: 'grantedAt',
            render: (v: string | null) => (v ? new Date(v).toLocaleDateString() : '-'),
          },
        ]}
      />
    </>
  );
}