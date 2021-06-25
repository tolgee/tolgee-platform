import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { PermissionsMenu } from 'tg.component/security/PermissionsMenu';
import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { useUser } from 'tg.hooks/useUser';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';

const messageService = container.resolve(MessageService);

const ProjectPermissionMenu = (props: {
  user: components['schemas']['UserAccountInProjectModel'];
}) => {
  const project = useProject();
  const currentUser = useUser();

  const editPermission = useApiMutation({
    url: '/v2/projects/{projectId}/users/{userId}/set-permissions/{permissionType}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/users',
  });

  return (
    <PermissionsMenu
      selected={
        props.user.computedPermissions ||
        project.organizationOwnerBasePermissions!
      }
      onSelect={(permissionType) => {
        confirmation({
          message: <T>change_permissions_confirmation</T>,
          onConfirm: () =>
            editPermission.mutate(
              {
                path: {
                  userId: props.user?.id,
                  permissionType,
                  projectId: project.id,
                },
              },
              {
                onSuccess() {
                  messageService.success(<T>permissions_set_message</T>);
                },
              }
            ),
        });
      }}
      buttonProps={{
        size: 'small',
        disabled: currentUser?.id === props.user.id,
      }}
      minPermissions={props.user.organizationBasePermissions}
    />
  );
};

export default ProjectPermissionMenu;
