import React, { ComponentProps, FunctionComponent } from 'react';
import { Button, Tooltip } from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { PermissionsModal } from './PermissionsModal';
import { PermissionModel } from '../../../../component/PermissionsSettings/types';

type Props = {
  buttonTooltip?: string;
  // onSelect: (value: PermissionType) => void;
  buttonProps?: ComponentProps<typeof Button>;
  user: { name?: string; id: number };
  permissions: PermissionModel;
};

export const PermissionsMenu: FunctionComponent<Props> = ({
  buttonTooltip,
  buttonProps,
  user,
  permissions,
}) => {
  const [open, setOpen] = React.useState(false);

  const handleClose = () => {
    setOpen(false);
  };

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setOpen(true);
  };

  return (
    <>
      <Tooltip title={buttonTooltip || ''} disableInteractive>
        <span>
          <Button
            data-cy="permissions-menu-button"
            {...buttonProps}
            variant="outlined"
            aria-haspopup="true"
            onClick={handleClick}
          >
            {permissions.type} <ArrowDropDown fontSize="small" />
          </Button>
        </span>
      </Tooltip>
      {open && (
        <PermissionsModal
          user={user}
          onClose={handleClose}
          permissions={permissions}
        />
      )}
    </>
  );
};
