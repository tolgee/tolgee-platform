import React, { useState } from 'react';
import { IconButton, MenuItem, Popover, styled } from '@mui/material';

import { UserAvatar } from 'tg.component/common/avatar/UserAvatar';

import { ThemeItem } from './ThemeItem';
import { LanguageItem } from './LanguageItem';
import { T } from '@tolgee/react';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { UserMenuItems } from './UserMenuItems';

const StyledIconButton = styled(IconButton)`
  width: 40px;
  height: 40px;
`;

const StyledPopover = styled(Popover)`
  & .paper {
    margin-top: 5px;
    padding: 2px 0px;
  }
`;

const StyledDivider = styled('div')`
  height: 1px;
  background: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.divider1
      : theme.palette.emphasis[400]};
`;

export const UserUnverifiedEmailMenu = () => {
  const [anchorEl, setAnchorEl] = useState(null);
  const { logout } = useGlobalActions();

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    //@ts-ignore
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  return (
    <div>
      <StyledIconButton
        color="inherit"
        data-cy="global-user-menu-button"
        aria-controls="user-menu"
        aria-haspopup="true"
        onClick={handleOpen}
        size="large"
      >
        <UserAvatar />
      </StyledIconButton>
      <StyledPopover
        id="user-menu"
        keepMounted
        open={!!anchorEl}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        classes={{ paper: 'paper' }}
      >
        <UserMenuItems onClose={handleClose} />

        <StyledDivider />
        <LanguageItem />
        <ThemeItem />
        <MenuItem onClick={() => logout()} data-cy="user-menu-logout">
          <T keyName="user_menu_logout" />
        </MenuItem>
      </StyledPopover>
    </div>
  );
};
