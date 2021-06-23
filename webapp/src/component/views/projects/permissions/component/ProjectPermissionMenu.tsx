import { PermissionsMenu } from '../../../../security/PermissionsMenu';
import { useProject } from '../../../../../hooks/useProject';
import { components } from '../../../../../service/apiSchema.generated';
import { useUser } from '../../../../../hooks/useUser';
import { container } from 'tsyringe';
import { confirmation } from '../../../../../hooks/confirmation';
import { T } from '@tolgee/react';
import { useApiMutation } from '../../../../../service/http/useQueryApi';
import { MessageService } from '../../../../../service/MessageService';

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
