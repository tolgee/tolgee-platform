import { useTranslate } from '@tolgee/react';
import { useLocation } from 'react-router-dom';

import { LINKS } from '../constants/links';
import {
  useConfig,
  useIsEmailVerified,
  useUser,
} from 'tg.globalContext/helpers';

export class UserMenuItem {
  constructor(
    public link: string,
    public label: string,
    public isSelected: boolean
  ) {}
}

export const useUserMenuItems = (): UserMenuItem[] => {
  const location = useLocation();
  const { t } = useTranslate();

  const config = useConfig();
  const user = useUser();
  const isEmailVerified = useIsEmailVerified();

  const userSettings =
    !config.authentication || !user
      ? []
      : [
          {
            link: LINKS.USER_PROFILE.build(),
            label: t('user_menu_user_settings'),
          },
        ];

  if (!isEmailVerified) {
    return [...userSettings].map((i) => {
      return new UserMenuItem(i.link, i.label, location.pathname === i.link);
    });
  }

  return [
    {
      link: LINKS.MY_TASKS.build(),
      label: t('user_menu_my_tasks'),
    },
    ...userSettings,
    {
      link: LINKS.USER_API_KEYS.build(),
      label: t('user_menu_api_keys'),
    },
    {
      link: LINKS.USER_PATS.build(),
      label: t('user_menu_pats'),
    },
  ].map((i) => {
    return new UserMenuItem(i.link, i.label, location.pathname === i.link);
  });
};
