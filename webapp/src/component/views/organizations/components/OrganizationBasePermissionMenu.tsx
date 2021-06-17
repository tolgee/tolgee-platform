import { useRouteMatch } from 'react-router-dom';
import { T } from '@tolgee/react';
import { FunctionComponent } from 'react';
import { components } from '../../../../service/apiSchema.generated';
import { container } from 'tsyringe';
import { MessageService } from '../../../../service/MessageService';
import { confirmation } from '../../../../hooks/confirmation';
import { PermissionsMenu } from '../../../security/PermissionsMenu';
import {
  Permission,
  useGetOrganization,
  usePutOrganization,
} from '../../../../service/hooks/Organization';
import { PARAMS } from '../../../../constants/links';

const messageService = container.resolve(MessageService);

export const OrganizationBasePermissionMenu: FunctionComponent<{
  organization: components['schemas']['OrganizationModel'];
}> = (props) => {
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];

  const organization = useGetOrganization(organizationSlug);
  const editOrganization = usePutOrganization(organization.data!.id);

  const handleSet = (type: Permission) => {
    confirmation({
      message: <T>really_want_to_change_base_permission_confirmation</T>,
      hardModeText: organization.data?.name?.toUpperCase(),
      onConfirm: () => {
        const dto: components['schemas']['OrganizationDto'] = {
          name: organization.data!.name,
          slug: organization.data?.slug,
          basePermissions: type,
          description: organization.data?.description,
        };
        editOrganization.mutate(dto, {
          onSuccess: () => {
            messageService.success(<T>organization_member_privileges_set</T>);
            organization.refetch();
          },
        });
      },
    });
  };

  return (
    <PermissionsMenu
      onSelect={handleSet}
      selected={organization.data!.basePermissions}
    />
  );
};
