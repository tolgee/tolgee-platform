import { Button, MenuProps } from '@material-ui/core';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import withStyles from '@material-ui/core/styles/withStyles';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import PersonIcon from '@material-ui/icons/Person';
import { T } from '@tolgee/react';
import { default as React, FunctionComponent, useState } from 'react';
import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import { useConfig } from 'tg.hooks/useConfig';
import { useUser } from 'tg.hooks/useUser';
import { useUserMenuItems } from 'tg.hooks/useUserMenuItems';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';
import { container } from 'tsyringe';

interface UserMenuProps {
  variant: 'small' | 'expanded';
}

const globalActions = container.resolve(GlobalActions);

export const UserMenu: FunctionComponent<UserMenuProps> = (props) => {
  const userLogged = useSelector(
    (state: AppState) => state.global.security.allowPrivate
  );

  const authentication = useConfig().authentication;

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

  const StyledMenu = withStyles({
    paper: {
      border: '1px solid #d3d4d5',
    },
  })((props: MenuProps) => (
    <Menu
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
      {...props}
    />
  ));

  return (
    <>
      {userLogged && (
        <div>
          <Button
            style={{ padding: 0 }}
            endIcon={<KeyboardArrowDownIcon />}
            color="inherit"
            data-cy="global-user-menu-button"
            aria-controls="user-menu"
            aria-haspopup="true"
            onClick={handleOpen}
          >
            {props.variant == 'expanded' ? user.name : <PersonIcon />}
          </Button>
          <StyledMenu
            id="user-menu"
            keepMounted
            open={!!anchorEl}
            anchorEl={anchorEl}
            onClose={handleClose}
          >
            <MenuItem onClick={() => globalActions.logout.dispatch()}>
              <T noWrap>user_menu_logout</T>
            </MenuItem>
            {userMenuItems.map((item, index) => (
              //@ts-ignore
              <MenuItem key={index} component={Link} to={item.link}>
                <T noWrap>{item.nameTranslationKey}</T>
              </MenuItem>
            ))}
          </StyledMenu>
        </div>
      )}
    </>
  );
};
