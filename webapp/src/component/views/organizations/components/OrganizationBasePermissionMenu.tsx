import { T } from '@tolgee/react';
import * as React from 'react';
import { FunctionComponent } from 'react';
import { components } from '../../../../service/apiSchema';
import { container } from 'tsyringe';
import { OrganizationActions } from '../../../../store/organization/OrganizationActions';
import { useOrganization } from '../../../../hooks/organizations/useOrganization';
import { confirmation } from '../../../../hooks/confirmation';
import { PermissionsMenu } from '../../../security/PermissionsMenu';

const actions = container.resolve(OrganizationActions);

export const OrganizationBasePermissionMenu: FunctionComponent<{
  organization: components['schemas']['OrganizationModel'];
}> = (props) => {
  const organization = useOrganization();

  const handleSet = (
    type: components['schemas']['OrganizationDto']['basePermissions']
  ) => {
    confirmation({
      message: <T>really_want_to_change_base_permission_confirmation</T>,
      hardModeText: organization.name.toUpperCase(),
      onConfirm: () => {
        const dto: components['schemas']['OrganizationDto'] = {
          name: organization.name,
          slug: organization.slug,
          basePermissions: type,
          description: organization.description,
        };
        actions.loadableActions.setMemberPrivileges.dispatch(
          organization.id,
          dto
        );
      },
    });
  };

  return (
    <PermissionsMenu
      onSelect={handleSet}
      selected={organization.basePermissions}
    />
  );
};
