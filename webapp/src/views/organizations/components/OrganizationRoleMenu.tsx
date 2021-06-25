import { FunctionComponent, useState } from 'react';
import { Button, Menu, MenuItem } from '@material-ui/core';
import { ArrowDropDown } from '@material-ui/icons';
import { T } from '@tolgee/react';
import { useQueryClient } from 'react-query';
import { container } from 'tsyringe';

import { confirmation } from 'tg.hooks/confirmation';
import { useUser } from 'tg.hooks/useUser';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { OrganizationRoleType } from 'tg.service/response.types';

import { useOrganization } from '../useOrganization';

const messagingService = container.resolve(MessageService);

export const OrganizationRoleMenu: FunctionComponent<{
  user: components['schemas']['UserAccountWithOrganizationRoleModel'];
}> = (props) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const queryClient = useQueryClient();

  const currentUser = useUser();
  const organization = useOrganization();
  const setRole = useApiMutation({
    url: '/v2/organizations/{organizationId}/users/{userId}/set-role',
    method: 'put',
  });

  const handleClose = () => {
    setAnchorEl(null);
  };

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
          }
        ),
    });

    handleClose();
  };

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  return (
    <>
      <Button
        data-cy="organization-role-menu-button"
        disabled={currentUser?.id == props.user.id}
        variant="outlined"
        size="small"
        aria-controls="simple-menu"
        aria-haspopup="true"
        onClick={handleClick}
      >
        <T>{`organization_role_type_${props.user.organizationRole}`}</T>{' '}
        <ArrowDropDown fontSize="small" />
      </Button>
      <Menu
        data-cy="organization-role-menu"
        elevation={1}
        id="simple-menu"
        anchorEl={anchorEl}
        getContentAnchorEl={null}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'center',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
      >
        {Object.keys(OrganizationRoleType).map((k) => (
          <MenuItem
            key={k}
            onClick={() => handleSet(k)}
            selected={k === props.user.organizationRole}
          >
            <T>{`organization_role_type_${k}`}</T>
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};
