import { MenuItem, MenuList, Paper } from '@material-ui/core';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { useUserMenuItems } from 'tg.hooks/useUserMenuItems';

export const UserSettingsMenu = () => {
  const menuItems = useUserMenuItems();

  return (
    <Paper elevation={0} variant="outlined" data-cy="user-account-side-menu">
      <MenuList>
        {menuItems.map((mi, idx) => (
          <MenuItem
            key={idx}
            selected={mi.isSelected}
            component={Link}
            to={mi.link}
          >
            <T>{mi.nameTranslationKey}</T>
          </MenuItem>
        ))}
      </MenuList>
    </Paper>
  );
};
