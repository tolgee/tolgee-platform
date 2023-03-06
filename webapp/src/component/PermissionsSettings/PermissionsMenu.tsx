import React, { ComponentProps, FunctionComponent } from 'react';
import { Button, Tooltip } from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { PermissionsModal } from './PermissionsModal';
import {
  LanguageModel,
  PermissionModel,
  PermissionSettingsState,
} from './types';
import { useRoleTranslations } from 'tg.component/PermissionsSettings/useRoleTranslations';

type Props = {
  allLangs?: LanguageModel[];
  buttonTooltip?: string;
  title: string;
  buttonProps?: ComponentProps<typeof Button>;
  permissions: PermissionModel;
  onSubmit: (settings: PermissionSettingsState) => Promise<void>;
};

export const PermissionsMenu: FunctionComponent<Props> = ({
  allLangs,
  buttonTooltip,
  buttonProps,
  title,
  permissions,
  onSubmit,
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
            {getRoleTranslation(permissions.type || 'ADVANCED')}
            <ArrowDropDown fontSize="small" />
          </Button>
        </span>
      </Tooltip>
      {open && (
        <PermissionsModal
          allLangs={allLangs}
          title={title}
          onClose={handleClose}
          permissions={permissions}
          onSubmit={onSubmit}
        />
      )}
    </>
  );
};
