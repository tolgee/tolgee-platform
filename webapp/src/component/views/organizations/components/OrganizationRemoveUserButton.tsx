import React from 'react';
import { Button } from '@material-ui/core';
import { T } from '@tolgee/react';
import { OrganizationActions } from '../../../../store/organization/OrganizationActions';
import { container } from 'tsyringe';
import { useOrganization } from '../../../../hooks/organizations/useOrganization';
import { confirmation } from '../../../../hooks/confirmation';

const actions = container.resolve(OrganizationActions);

const OrganizationRemoveUserButton = (props: {
  userId: number;
  userName: string;
}) => {
  const organization = useOrganization();

  const removeUser = () => {
    confirmation({
      message: (
        <T parameters={{ userName: props.userName }}>
          really_remove_user_confirmation
        </T>
      ),
      onConfirm: () =>
        actions.loadableActions.removeUser.dispatch(
          organization.id,
          props.userId
        ),
    });
  };

  return (
    <Button
      data-cy="organization-members-remove-user-button"
      onClick={removeUser}
      variant="outlined"
      size="small"
      aria-controls="simple-menu"
      aria-haspopup="true"
    >
      <T>organization_users_remove_user</T>
    </Button>
  );
};

export default OrganizationRemoveUserButton;
