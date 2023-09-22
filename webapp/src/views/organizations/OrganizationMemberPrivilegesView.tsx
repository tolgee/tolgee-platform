import { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { useTranslate, T } from '@tolgee/react';

import { BaseOrganizationSettingsView } from './components/BaseOrganizationSettingsView';
import { OrganizationBasePermissionMenu } from './components/OrganizationBasePermissionMenu';
import { useOrganization } from './useOrganization';
import { LINKS, PARAMS } from 'tg.constants/links';
import { ScopesInfo } from 'tg.component/PermissionsSettings/ScopesInfo';

export const OrganizationMemberPrivilegesView: FunctionComponent = () => {
  const organization = useOrganization();
  const { t } = useTranslate();

  return (
    <BaseOrganizationSettingsView
      windowTitle={t('organization_member_privileges_title')}
      title={t('organization_member_privileges_title')}
      link={LINKS.ORGANIZATION_MEMBER_PRIVILEGES}
      maxWidth="normal"
      navigation={[
        [
          t('organization_member_privileges_title'),
          LINKS.ORGANIZATION_MEMBER_PRIVILEGES.build({
            [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
          }),
        ],
      ]}
    >
      <Typography variant="body1">
        <T keyName="organization_member_privileges_text" />
      </Typography>

      <Box mt={2} display="flex" gap="8px" alignItems="center">
        <OrganizationBasePermissionMenu organization={organization!} />
        <ScopesInfo scopes={organization!.basePermissions.scopes} />
      </Box>
    </BaseOrganizationSettingsView>
  );
};
