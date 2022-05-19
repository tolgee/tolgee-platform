import { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { useTranslate, T } from '@tolgee/react';

import { BaseOrganizationSettingsView } from './BaseOrganizationSettingsView';
import { OrganizationBasePermissionMenu } from './components/OrganizationBasePermissionMenu';
import { useOrganization } from './useOrganization';

export const OrganizationMemberPrivilegesView: FunctionComponent = () => {
  const organization = useOrganization();
  const t = useTranslate();

  return (
    <BaseOrganizationSettingsView
      windowTitle={t('organization_member_privileges_title')}
      title={t('organization_member_privileges_title')}
    >
      <Typography variant="body1">
        <T>organization_member_privileges_text</T>
      </Typography>

      <Box mt={2}>
        <OrganizationBasePermissionMenu organization={organization!} />
      </Box>
    </BaseOrganizationSettingsView>
  );
};
