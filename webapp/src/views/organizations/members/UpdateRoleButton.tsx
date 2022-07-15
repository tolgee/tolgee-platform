import { FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import { useQueryClient } from 'react-query';
import { container } from 'tsyringe';

import { confirmation } from 'tg.hooks/confirmation';
import { useUser } from 'tg.globalContext/helpers';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { RoleMenu } from 'tg.component/security/RoleMenu';
import { useOrganization } from '../useOrganization';

const messagingService = container.resolve(MessageService);

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
      message: <T>really_want_to_change_role_confirmation</T>,
      onConfirm: () =>
        setRole.mutate(
          {
            path: { organizationId: organization!.id, userId: props.user.id },
            content: { 'application/json': { roleType: type } },
          },
          {
            onSuccess: () => {
              messagingService.success(
                <T>organization_role_changed_message</T>
              );
              queryClient.invalidateQueries([]);
            },
            onError(e) {
              parseErrorResponse(e).forEach((err) =>
                messagingService.error(<T>{err}</T>)
              );
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
