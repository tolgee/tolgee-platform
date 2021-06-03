import React from 'react';
import { PermissionsMenu } from '../../../../security/PermissionsMenu';
import { useProject } from '../../../../../hooks/useProject';
import { components } from '../../../../../service/apiSchema';
import { useUser } from '../../../../../hooks/useUser';
import { container } from 'tsyringe';
import { ProjectActions } from '../../../../../store/project/ProjectActions';
import { confirmation } from '../../../../../hooks/confirmation';
import { T } from '@tolgee/react';

const projectActions = container.resolve(ProjectActions);
const ProjectPermissionMenu = (props: {
  user: components['schemas']['UserAccountInProjectModel'];
}) => {
  const project = useProject();
  const currentUser = useUser();

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
            projectActions.loadableActions.setUsersPermissions.dispatch({
              path: {
                userId: props.user?.id!!,
                permissionType,
                projectId: project.id,
              },
            }),
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
