import { FunctionComponent, ReactElement } from 'react';
import { IconButton, Tooltip } from '@material-ui/core';
import { Clear } from '@material-ui/icons';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { useUser } from 'tg.hooks/useUser';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';

const messageService = container.resolve(MessageService);

const RevokePermissionsButton = (props: {
  user: components['schemas']['UserAccountInProjectModel'];
}) => {
  const hasOrganizationRole = !!props.user.organizationRole;
  const project = useProject();
  const currentUser = useUser();

  const revokeAccess = useApiMutation({
    url: '/v2/projects/{projectId}/users/{userId}/revoke-access',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/users',
  });

  const handleRevoke = () => {
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
  };

  let disabledTooltipTitle = undefined as ReactElement | undefined;

  if (currentUser!.id === props.user.id) {
    disabledTooltipTitle = <T>cannot_revoke_your_own_access_tooltip</T>;
  } else if (hasOrganizationRole) {
    disabledTooltipTitle = <T>user_is_part_of_organization_tooltip</T>;
  }

  const isDisabled = !!disabledTooltipTitle;

  const Wrapper: FunctionComponent = (props) =>
    !isDisabled ? (
      <>{props.children}</>
    ) : (
      <Tooltip title={disabledTooltipTitle!}>
        <span>{props.children}</span>
      </Tooltip>
    );

  return (
    <Wrapper>
      <IconButton
        data-cy="project-member-revoke-button"
        disabled={isDisabled}
        size="small"
        onClick={() =>
          confirmation({
            title: <T>revoke_access_confirmation_title</T>,
            message: (
              <T
                parameters={{
                  userName: props.user.name || props.user.username!,
                }}
              >
                project_permissions_revoke_user_access_message
              </T>
            ),
            onConfirm: handleRevoke,
          })
        }
      >
        <Clear />
      </IconButton>
    </Wrapper>
  );
};

export default RevokePermissionsButton;
