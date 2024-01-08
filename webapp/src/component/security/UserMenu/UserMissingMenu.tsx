import React, { useState } from 'react';
import { IconButton, MenuItem, Popover, styled } from '@mui/material';
import { Link } from 'react-router-dom';

import { useUserMenuItems } from 'tg.hooks/useUserMenuItems';
import { UserAvatar } from 'tg.component/common/avatar/UserAvatar';

import { ThemeItem } from './ThemeItem';
import { LanguageItem } from './LanguageItem';

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

export const UserMissingMenu: React.FC = () => {
  const [anchorEl, setAnchorEl] = useState(null);
  const userMenuItems = useUserMenuItems();

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
        {userMenuItems.map((item, index) => (
          <MenuItem
            key={index}
            component={Link}
            to={item.link}
            selected={item.isSelected}
            onClick={handleClose}
            data-cy="user-menu-user-settings"
          >
            {item.label}
          </MenuItem>
        ))}

        <StyledDivider />
        <LanguageItem />
        <ThemeItem />
      </StyledPopover>
    </div>
  );
};
