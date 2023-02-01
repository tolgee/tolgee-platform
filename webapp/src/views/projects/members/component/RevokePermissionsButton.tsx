import { FunctionComponent, ReactElement } from 'react';
import { IconButton, Tooltip } from '@mui/material';
import { Clear } from '@mui/icons-material';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { useUser } from 'tg.globalContext/helpers';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useLeaveProject } from 'tg.views/projects/useLeaveProject';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

const messageService = container.resolve(MessageService);

const RevokePermissionsButton = (props: {
  user: components['schemas']['UserAccountInProjectModel'];
}) => {
  const hasOrganizationRole = !!props.user.organizationRole;
  const project = useProject();
  const currentUser = useUser();
  const { satisfiesPermission } = useProjectPermissions();
  const isAdmin = satisfiesPermission('admin');

  const { leave, isLeaving } = useLeaveProject();

  const revokeAccess = useApiMutation({
    url: '/v2/projects/{projectId}/users/{userId}/revoke-access',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/users',
  });

  const handleRevoke = () => {
    if (currentUser!.id === props.user.id) {
      leave(project.name, project.id);
    } else {
      confirmation({
        title: <T>revoke_access_confirmation_title</T>,
        message: (
          <T
            params={{
              userName: props.user.name || props.user.username!,
            }}
          >
            project_permissions_revoke_user_access_message
          </T>
        ),
        onConfirm: () => {
          revokeAccess.mutate(
            {
              path: {
                projectId: project.id,
                userId: props.user.id,
              },
            },
            {
              onSuccess() {
                messageService.success(<T>access_revoked_message</T>);
              },
            }
          );
        },
      });
    }
  };

  useGlobalLoading(isLeaving || revokeAccess.isLoading);

  let isDisabled = false;
  let tooltip = undefined as ReactElement | undefined;

  if (hasOrganizationRole) {
    tooltip = <T keyName="user_is_part_of_organization_tooltip" />;
    isDisabled = true;
  } else if (currentUser!.id === props.user.id) {
    tooltip = <T keyName="project_leave_button" />;
  } else if (!isAdmin) {
    tooltip = <T keyName="operation_not_permitted_error" />;
    isDisabled = true;
  }

  const Wrapper: FunctionComponent = (props) =>
    !tooltip ? (
      <>{props.children}</>
    ) : (
      <Tooltip title={tooltip}>
        <span>{props.children}</span>
      </Tooltip>
    );

  return (
    <Wrapper>
      <IconButton
        data-cy="project-member-revoke-button"
        disabled={isDisabled}
        size="small"
        onClick={handleRevoke}
      >
        <Clear />
      </IconButton>
    </Wrapper>
  );
};

export default RevokePermissionsButton;
