import React, { ComponentProps, FunctionComponent } from 'react';
import { Button, Tooltip } from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { PermissionsModal } from '../PermissionsModal/PermissionsModal';
import { PermissionModel } from './types';

type Props = {
  buttonTooltip?: string;
  // onSelect: (value: PermissionType) => void;
  buttonProps?: ComponentProps<typeof Button>;
  nameInTitle?: string;
  permissions: PermissionModel;
};

export const PermissionsMenu: FunctionComponent<Props> = ({
  buttonTooltip,
  buttonProps,
  nameInTitle,
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
          nameInTitle={nameInTitle}
          onClose={handleClose}
          permissions={permissions}
        />
      )}
    </>
  );
};
