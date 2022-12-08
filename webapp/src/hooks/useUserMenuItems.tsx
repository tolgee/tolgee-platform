import { useTranslate } from '@tolgee/react';
import { useLocation } from 'react-router-dom';

import { LINKS } from '../constants/links';

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

  return [
    {
      link: LINKS.USER_PROFILE.build(),
      label: t('user_menu_user_settings'),
    },
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
