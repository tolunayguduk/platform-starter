import { Link } from 'react-router-dom';
import { Avatar, Space, Typography } from 'antd';
import { TeamOutlined } from '@ant-design/icons';

/** An organization's name, wherever it's shown in Settings or the admin panel - clicking it
 * always jumps to that organization's public landing page. */
export function OrganizationNameLink({ id, name, logoImageUrl }: { id: string; name: string; logoImageUrl?: string | null }) {
  return (
    <Link to={`/organizations/${id}`} onClick={(e) => e.stopPropagation()}>
      <Space size={8}>
        <Avatar size="small" shape="square" src={logoImageUrl || undefined} icon={<TeamOutlined />} />
        <Typography.Text>{name}</Typography.Text>
      </Space>
    </Link>
  );
}
