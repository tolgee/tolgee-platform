import { FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';
import { container } from 'tsyringe';

import { PermissionsMenu } from 'tg.component/security/PermissionsMenu';
import { PARAMS } from 'tg.constants/links';
import { confirmation } from 'tg.hooks/confirmation';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';

type Permission = components['schemas']['OrganizationDto']['basePermissions'];
type OrganizationModel = components['schemas']['OrganizationModel'];
type OrganizationDto = components['schemas']['OrganizationDto'];

const messageService = container.resolve(MessageService);

export const OrganizationBasePermissionMenu: FunctionComponent<{
  organization: OrganizationModel;
}> = (props) => {
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];

  const organization = useApiQuery({
    url: '/v2/organizations/{slug}',
    method: 'get',
    path: { slug: organizationSlug },
  });
  const editOrganization = useApiMutation({
    url: '/v2/organizations/{id}',
    method: 'put',
  });

  const handleSet = (type: Permission) => {
    confirmation({
      message: <T>really_want_to_change_base_permission_confirmation</T>,
      hardModeText: organization.data?.name?.toUpperCase(),
      onConfirm: () => {
        const dto: OrganizationDto = {
          name: organization.data!.name,
          slug: organization.data?.slug,
          basePermissions: type,
          description: organization.data?.description,
        };
        editOrganization.mutate(
          {
            path: { id: organization.data!.id },
            content: { 'application/json': dto },
          },
          {
            onSuccess: () => {
              messageService.success(<T>organization_member_privileges_set</T>);
              organization.refetch();
            },
          }
        );
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
