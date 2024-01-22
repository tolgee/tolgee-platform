import { FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import { useQueryClient } from 'react-query';

import { confirmation } from 'tg.hooks/confirmation';
import { useUser } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { RoleMenu } from 'tg.component/security/RoleMenu';
import { messageService } from 'tg.service/MessageService';

import { useOrganization } from '../useOrganization';

export const UpdateRoleButton: FunctionComponent<{
  user: components['schemas']['UserAccountWithOrganizationRoleModel'];
}> = (props) => {
  const queryClient = useQueryClient();

  const currentUser = useUser();
  const organization = useOrganization();
  const setRole = useApiMutation({
    url: '/v2/organizations/{organizationId}/users/{userId}/set-role',
    method: 'put',
  });

  const handleSet = (type) => {
    confirmation({
      message: <T keyName="really_want_to_change_role_confirmation" />,
      onConfirm: () =>
        setRole.mutate(
          {
            path: { organizationId: organization!.id, userId: props.user.id },
            content: { 'application/json': { roleType: type } },
          },
          {
            onSuccess: () => {
              messageService.success(
                <T keyName="organization_role_changed_message" />
              );
              queryClient.invalidateQueries([]);
            },
          }
        ),
    });
  };

  return (
    <RoleMenu
      onSelect={(role) => handleSet(role)}
      role={props.user.organizationRole}
      buttonProps={{ disabled: props.user.id === currentUser?.id }}
    />
  );
};
