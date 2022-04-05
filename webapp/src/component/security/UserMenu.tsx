import { default as React, useState } from 'react';
import { IconButton, Menu, MenuItem, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import { container } from 'tsyringe';

import { useConfig } from 'tg.hooks/useConfig';
import { useUser } from 'tg.hooks/useUser';
import { useUserMenuItems } from 'tg.hooks/useUserMenuItems';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';
import { UserAvatar } from '../common/avatar/UserAvatar';

const globalActions = container.resolve(GlobalActions);

const StyledIconButton = styled(IconButton)`
  width: 40px;
  height: 40px;
`;

const StyledMenu = styled(Menu)`
  & .paper {
    border: 1px solid #d3d4d5;
    margin-top: 5px;
  }
`;

const StyledMenuItem = styled(MenuItem)`
  opacity: 0.7 !important;
`;

export const UserMenu: React.FC = () => {
  const userLogged = useSelector(
    (state: AppState) => state.global.security.allowPrivate
  );

  const { authentication } = useConfig();

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    //@ts-ignore
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const [anchorEl, setAnchorEl] = useState(null);

  const user = useUser();
  const userMenuItems = useUserMenuItems();

  if (!authentication || !user) {
    return null;
  }

  return userLogged ? (
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
      <StyledMenu
        id="user-menu"
        keepMounted
        open={!!anchorEl}
        anchorEl={anchorEl}
        onClose={handleClose}
        elevation={0}
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
        <StyledMenuItem divider disabled>
          {user.name}
        </StyledMenuItem>
        {userMenuItems.map((item, index) => (
          //@ts-ignore
          <MenuItem key={index} component={Link} to={item.link}>
            <T noWrap>{item.nameTranslationKey}</T>
          </MenuItem>
        ))}
        <MenuItem onClick={() => globalActions.logout.dispatch()}>
          <T noWrap>user_menu_logout</T>
        </MenuItem>
      </StyledMenu>
    </div>
  ) : null;
};
