import { useTranslate } from '@tolgee/react';
import { Link, useLocation } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';
import {
  useConfig,
  useIsEmailVerified,
  useUser,
} from 'tg.globalContext/helpers';
import { MenuItem } from '@mui/material';
import React, { FC } from 'react';
import { createAdder } from 'tg.fixtures/pluginAdder';
import { useAddUserMenuItems } from 'tg.ee';

export const UserMenuItems: FC<{ onClose: () => void }> = ({ onClose }) => {
  const location = useLocation();
  const { t } = useTranslate();

  const config = useConfig();
  const user = useUser();
  const isEmailVerified = useIsEmailVerified();

  const addEeItems = useAddUserMenuItems();

  const baseItems = [
    {
      id: 'user-settings',
      enabled: config.authentication && !!user,
      Component: (props: { onClose: () => void }) => (
        <MenuItem
          component={Link}
          to={LINKS.USER_PROFILE.build()}
          selected={location.pathname === LINKS.USER_PROFILE.build()}
          onClick={props.onClose}
          data-cy="user-menu-user-settings"
        >
          {t('user_menu_user_settings')}
        </MenuItem>
      ),
    },
    {
      id: 'api-keys',
      enabled: isEmailVerified,
      Component: (props: { onClose: () => void }) => (
        <MenuItem
          component={Link}
          to={LINKS.USER_API_KEYS.build()}
          selected={location.pathname === LINKS.USER_API_KEYS.build()}
          onClick={props.onClose}
          data-cy="user-menu-user-settings"
        >
          {t('user_menu_api_keys')}
        </MenuItem>
      ),
    },
    {
      id: 'pats',
      enabled: isEmailVerified,
      Component: (props: { onClose: () => void }) => (
        <MenuItem
          component={Link}
          to={LINKS.USER_PATS.build()}
          selected={location.pathname === LINKS.USER_PATS.build()}
          onClick={props.onClose}
          data-cy="user-menu-user-settings"
        >
          {t('user_menu_pats')}
        </MenuItem>
      ),
    },
  ] satisfies UserMenuItem[];

  const items = addEeItems(baseItems);

  return (
    <>
      {items.map(({ Component, enabled }, index) => {
        if (!enabled) return null;
        return <Component key={index} onClose={onClose} />;
      })}
    </>
  );
};

type UserMenuItem = {
  id: string;
  enabled: boolean;
  Component: React.FC<{ onClose: () => void }>;
};

export const addUserMenuItems = createAdder<UserMenuItem>({
  referencingProperty: 'id',
});

export type UserMenuItemsAdder = ReturnType<typeof addUserMenuItems>;
