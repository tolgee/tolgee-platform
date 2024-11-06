import { FunctionComponent, ReactElement } from 'react';
import { IconButton, Tooltip } from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { useUser } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useLeaveProject } from 'tg.views/projects/useLeaveProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { messageService } from 'tg.service/MessageService';

const RevokePermissionsButton = (props: {
  user: components['schemas']['UserAccountInProjectModel'];
}) => {
  const hasOrganizationRole = !!props.user.organizationRole;
  const project = useProject();
  const currentUser = useUser();
  const { satisfiesPermission } = useProjectPermissions();
  const canEditMembers = satisfiesPermission('members.edit');

  const { leave } = useLeaveProject();

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
        title: <T keyName="revoke_access_confirmation_title" />,
        message: (
          <T
            keyName="project_permissions_revoke_user_access_message"
            params={{
              userName: props.user.name || props.user.username!,
            }}
          />
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
                messageService.success(<T keyName="access_revoked_message" />);
              },
            }
          );
        },
      });
    }
  };

  let isDisabled = false;
  let tooltip = undefined as ReactElement | undefined;

  if (hasOrganizationRole) {
    tooltip = <T keyName="user_is_part_of_organization_tooltip" />;
    isDisabled = true;
  } else if (currentUser!.id === props.user.id) {
    tooltip = <T keyName="project_leave_button" />;
  } else if (!canEditMembers) {
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
        <XClose />
      </IconButton>
    </Wrapper>
  );
};

export default RevokePermissionsButton;
