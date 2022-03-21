import React, { ComponentProps, FunctionComponent } from 'react';
import { Button, Menu, MenuItem } from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { T } from '@tolgee/react';

import { ProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { components } from 'tg.service/apiSchema.generated';
import { ProjectPermissionType } from 'tg.service/response.types';

export const PermissionsMenu: FunctionComponent<{
  selected: NonNullable<
    components['schemas']['ProjectModel']['computedPermissions']
  >;
  onSelect: (
    value: NonNullable<
      components['schemas']['ProjectModel']['computedPermissions']
    >
  ) => void;
  buttonProps?: ComponentProps<typeof Button>;
  minPermissions?: NonNullable<
    components['schemas']['ProjectModel']['computedPermissions']
  >;
}> = (props) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  let types = Object.keys(ProjectPermissionType);

  if (props.minPermissions) {
    types = types.filter((k) =>
      new ProjectPermissions(k as any).satisfiesPermission(
        props.minPermissions as any
      )
    );
  }

  return (
    <>
      <Button
        data-cy="permissions-menu-button"
        {...props.buttonProps}
        variant="outlined"
        aria-controls="simple-menu"
        aria-haspopup="true"
        onClick={handleClick}
      >
        <T>{`permission_type_${props.selected.toLowerCase()}`}</T>{' '}
        <ArrowDropDown fontSize="small" />
      </Button>
      <Menu
        data-cy="permissions-menu"
        elevation={1}
        id="simple-menu"
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'center',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
      >
        {types.map((k) => (
          <MenuItem
            key={k}
            onClick={() => {
              props.onSelect(k as any);
              handleClose();
            }}
            disabled={k === props.selected}
            selected={k === props.selected}
          >
            <T>{`permission_type_${k.toLowerCase()}`}</T>
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};
