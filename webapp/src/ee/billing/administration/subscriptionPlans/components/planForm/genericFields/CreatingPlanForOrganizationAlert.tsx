import { FC } from 'react';
import { Alert } from '@mui/material';
import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

export const CreatingPlanForOrganizationAlert: FC<{
  organization?: components['schemas']['OrganizationModel'];
}> = ({ organization }) => {
  if (!organization) {
    return null;
  }

  return (
    <Alert
      sx={{ mt: 2, mb: 2 }}
      severity="info"
      data-cy="administration-billing-creating-for-organization-alert"
    >
      <T
        keyName="administration_cloud_plan_for_organization_alert"
        params={{
          organizationName: organization?.name,
          id: organization.id,
          slug: organization?.slug,
          b: <b />,
        }}
      />
    </Alert>
  );
};
