import React, {FunctionComponent, ReactElement} from 'react';
import {Button, Tooltip} from '@material-ui/core';
import {confirmation} from '../../../../../hooks/confirmation';
import {T} from '@tolgee/react';
import {components} from '../../../../../service/apiSchema';
import {useUser} from '../../../../../hooks/useUser';
import {container} from 'tsyringe';
import {ProjectActions} from '../../../../../store/project/ProjectActions';
import {useProject} from '../../../../../hooks/useProject';

const projectActions = container.resolve(ProjectActions);

const RevokePermissionsButton = (props: {
  user: components['schemas']['UserAccountInProjectModel'];
}) => {
  const hasOrganizationRole = !!props.user.organizationRole;
  const project = useProject();
  const currentUser = useUser();
  let disabledTooltipTitle = undefined as ReactElement | undefined;

  if (currentUser!.id === props.user.id) {
    disabledTooltipTitle = <T noWrap>cannot_revoke_your_own_access_tooltip</T>;
  } else if (hasOrganizationRole) {
    disabledTooltipTitle = <T noWrap>user_is_part_of_organization_tooltip</T>;
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
      <Button
        data-cy="permissions-revoke-button"
        disabled={isDisabled}
        size="small"
        variant="outlined"
        onClick={() =>
          confirmation({
            title: <T>revoke_access_confirmation_title</T>,
            message: (
              <T
                parameters={{
                  userName: props.user.name || props.user.username!!,
                }}
              >
                project_permissions_revoke_user_access_message
              </T>
            ),
            onConfirm: () => {
              projectActions.loadableActions.revokeAccess.dispatch({
                path: {
                  projectId: project.id,
                  userId: props.user.id,
                },
              });
            },
          })
        }
      >
        Revoke
      </Button>
    </Wrapper>
  );
};

export default RevokePermissionsButton;
