import { default as React, useState } from 'react';
import { IconButton, makeStyles, Menu, MenuItem } from '@material-ui/core';
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

const useStyles = makeStyles((theme) => ({
  paper: {
    border: '1px solid #d3d4d5',
    marginTop: 5,
  },
  iconButton: {
    width: 40,
    height: 40,
  },
  icon: {
    width: 30,
    height: 30,
  },
  menuTitle: {
    opacity: '0.7 !important',
  },
}));

export const UserMenu: React.FC = () => {
  const classes = useStyles();
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
      <IconButton
        color="inherit"
        data-cy="global-user-menu-button"
        aria-controls="user-menu"
        aria-haspopup="true"
        onClick={handleOpen}
        className={classes.iconButton}
      >
        <UserAvatar />
      </IconButton>
      <Menu
        id="user-menu"
        keepMounted
        open={!!anchorEl}
        anchorEl={anchorEl}
        onClose={handleClose}
        elevation={0}
        getContentAnchorEl={null}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        classes={{ paper: classes.paper }}
      >
        <MenuItem divider disabled className={classes.menuTitle}>
          {user.name}
        </MenuItem>
        {userMenuItems.map((item, index) => (
          //@ts-ignore
          <MenuItem key={index} component={Link} to={item.link}>
            <T noWrap>{item.nameTranslationKey}</T>
          </MenuItem>
        ))}
        <MenuItem onClick={() => globalActions.logout.dispatch()}>
          <T noWrap>user_menu_logout</T>
        </MenuItem>
      </Menu>
    </div>
  ) : null;
};
