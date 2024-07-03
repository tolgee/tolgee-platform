import React, { ComponentProps, FunctionComponent } from 'react';
import { Button, Tooltip } from '@mui/material';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { PermissionModalProps, PermissionsModal } from './PermissionsModal';
import { useRoleTranslations } from 'tg.component/PermissionsSettings/useRoleTranslations';

type Props = {
  buttonTooltip?: string;
  buttonProps?: ComponentProps<typeof Button>;
  modalProps: Omit<PermissionModalProps, 'onClose'>;
};

export const PermissionsMenu: FunctionComponent<Props> = ({
  buttonTooltip,
  buttonProps,
  modalProps,
}) => {
  const [open, setOpen] = React.useState(false);

  const handleClose = () => {
    setOpen(false);
  };

  const handleClick = () => {
    setOpen(true);
  };

  const { getRoleTranslation } = useRoleTranslations();

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
            {getRoleTranslation(modalProps.permissions.type || 'ADVANCED')}
            <ArrowDropDown fontSize="small" />
          </Button>
        </span>
      </Tooltip>
      {open && <PermissionsModal {...modalProps} onClose={handleClose} />}
    </>
  );
};
