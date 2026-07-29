import { Button } from 'antd';
import type { UiPermissionState } from '../api/permissions';

/**
 * ENABLED -> normal button, DISABLED -> disabled button, HIDDEN -> renders nothing.
 */
export function PermissionButton({
  state,
  label,
  onClick,
}: {
  state: UiPermissionState | undefined;
  label: string;
  onClick: () => void;
}) {
  if (state === 'HIDDEN' || state === undefined) {
    return null;
  }
  return (
    <Button disabled={state === 'DISABLED'} onClick={onClick}>
      {label}
    </Button>
  );
}
