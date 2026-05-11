import { Alert } from '@mui/material';
import { T } from '@tolgee/react';

type Props = {
  'data-cy'?: string;
  sx?: Record<string, any>;
};

export function AdminAccessAlert(props: Props) {
  return (
    <Alert severity="info" {...props}>
      <T
        keyName="machine_translation_admin_access_info"
        defaultValue="You are accessing this project as an admin. Credits will not be consumed and organization providers are unavailable."
      />
    </Alert>
  );
}
