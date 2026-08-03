import { useEffect, useState } from 'react';
import { AutoComplete, Avatar, Input, Typography } from 'antd';
import { TeamOutlined, SearchOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../store/AuthContext';
import { searchOrganizations } from '../api/organizationDirectoryApi';
import type { OrganizationSearchResult } from '../types/organization';
import styles from './OrganizationSearchBox.module.css';

/** How users find an organization's landing page - this and the org's own permanent invite link
 * are the only two ways in, now that AccountTab no longer has its own join form. */
export function OrganizationSearchBox() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { accessToken } = useAuth();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<OrganizationSearchResult[]>([]);

  useEffect(() => {
    if (!accessToken || !query.trim()) {
      setResults([]);
      return;
    }
    const handle = setTimeout(() => {
      searchOrganizations(accessToken, query.trim()).then(setResults);
    }, 300);
    return () => clearTimeout(handle);
  }, [accessToken, query]);

  function handleSelect(organizationId: string) {
    navigate(`/organizations/${organizationId}`);
    setQuery('');
    setResults([]);
  }

  return (
    <div className={styles.wrapper}>
      <AutoComplete
        className={styles.search}
        value={query}
        onChange={setQuery}
        onSelect={handleSelect}
        options={results.map((org) => ({
          value: org.id,
          label: (
            <div className={styles.resultRow}>
              <Avatar size="small" shape="square" src={org.logoImageUrl || undefined} icon={<TeamOutlined />} />
              <Typography.Text className={styles.resultName}>{org.name}</Typography.Text>
              <Typography.Text type="secondary" className={styles.resultCount}>
                {t('organization.memberCount', { count: org.memberCount })}
              </Typography.Text>
            </div>
          ),
        }))}
      >
        <Input
          className={styles.input}
          size="small"
          variant="borderless"
          placeholder={t('nav.searchOrganizations')}
          prefix={<SearchOutlined className={styles.icon} />}
          allowClear
        />
      </AutoComplete>
    </div>
  );
}
