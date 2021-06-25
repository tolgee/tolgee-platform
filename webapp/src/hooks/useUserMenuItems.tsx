import { useRouteMatch } from 'react-router-dom';

import { LINKS } from '../constants/links';

export class UserMenuItem {
  constructor(
    public link: string,
    public nameTranslationKey: string,
    public isSelected: boolean
  ) {}
}

export const useUserMenuItems = (): UserMenuItem[] => {
  const match = useRouteMatch();

  return [
    {
      link: LINKS.USER_SETTINGS.build(),
      nameTranslationKey: 'user_menu_user_settings',
    },
    {
      link: LINKS.USER_API_KEYS.build(),
      nameTranslationKey: 'user_menu_api_keys',
    },
    {
      link: LINKS.ORGANIZATIONS.build(),
      nameTranslationKey: 'user_menu_organizations',
    },
  ].map(
    (i) => new UserMenuItem(i.link, i.nameTranslationKey, match.url === i.link)
  );
};
