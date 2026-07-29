import { useRef } from 'react';
import { Form, Input, Avatar, Button, type InputRef } from 'antd';
import { UserOutlined, EditOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import styles from './AvatarNameFields.module.css';
import layoutStyles from './profileLayout.module.css';

/** Avatar preview (click the pencil badge to jump to the URL field) + first/last name. */
export function AvatarNameFields() {
  const { t } = useTranslation();
  const avatarInputRef = useRef<InputRef>(null);
  const avatarUrl = Form.useWatch('avatarUrl');

  return (
    <div className={styles.row}>
      <div className={styles.avatarWrap}>
        <Avatar
          shape="square"
          size={96}
          src={avatarUrl || undefined}
          icon={<UserOutlined />}
          className={styles.avatarImage}
        />
        <Button
          type="primary"
          shape="circle"
          size="small"
          icon={<EditOutlined />}
          onClick={() => avatarInputRef.current?.focus()}
          className={styles.editBadge}
        />
      </div>
      <div className={styles.fields}>
        <div className={layoutStyles.fieldRow}>
          <Form.Item
            name="firstName"
            label={t('profile.firstName')}
            rules={[{ required: true }]}
            className={`${layoutStyles.flexItem} ${layoutStyles.mb12}`}
          >
            <Input size="large" />
          </Form.Item>
          <Form.Item
            name="lastName"
            label={t('profile.lastName')}
            rules={[{ required: true }]}
            className={`${layoutStyles.flexItem} ${layoutStyles.mb12}`}
          >
            <Input size="large" />
          </Form.Item>
        </div>
        <Form.Item name="avatarUrl" label={t('profile.avatarUrl')} className={layoutStyles.mb0}>
          <Input ref={avatarInputRef} size="large" placeholder="https://..." />
        </Form.Item>
      </div>
    </div>
  );
}