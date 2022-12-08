import { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { useTranslate, T } from '@tolgee/react';

import { BaseOrganizationSettingsView } from './components/BaseOrganizationSettingsView';
import { OrganizationBasePermissionMenu } from './components/OrganizationBasePermissionMenu';
import { useOrganization } from './useOrganization';
import { LINKS, PARAMS } from 'tg.constants/links';

export const OrganizationMemberPrivilegesView: FunctionComponent = () => {
  const organization = useOrganization();
  const { t } = useTranslate();

  return (
    <BaseOrganizationSettingsView
      windowTitle={t('organization_member_privileges_title')}
      title={t('organization_member_privileges_title')}
      link={LINKS.ORGANIZATION_MEMBER_PRIVILEGES}
      containerMaxWidth="md"
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
        <T>organization_member_privileges_text</T>
      </Typography>

      <Box mt={2}>
        <OrganizationBasePermissionMenu organization={organization!} />
      </Box>
    </BaseOrganizationSettingsView>
  );
};
