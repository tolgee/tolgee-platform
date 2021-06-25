import { FunctionComponent } from 'react';
import { Box, Typography } from '@material-ui/core';
import { T } from '@tolgee/react';

import { BaseOrganizationSettingsView } from './BaseOrganizationSettingsView';
import { OrganizationBasePermissionMenu } from './components/OrganizationBasePermissionMenu';
import { useOrganization } from './useOrganization';

export const OrganizationMemberPrivilegesView: FunctionComponent = () => {
  const organization = useOrganization();

  return (
    <BaseOrganizationSettingsView
      title={<T>organization_member_privileges_title</T>}
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
