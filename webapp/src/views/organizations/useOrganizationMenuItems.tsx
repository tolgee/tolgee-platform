import { useRouteMatch } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';

import { useOrganization } from './useOrganization';

export class OrganizationMenuItem {
  constructor(
    public link: string,
    public nameTranslationKey: string,
    public isSelected: boolean
  ) {}
}

export const useOrganizationMenuItems = (): OrganizationMenuItem[] => {
  const match = useRouteMatch();
  const organization = useOrganization();

  return [
    {
      link: LINKS.ORGANIZATION_PROFILE.build({
        [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
      }),
      nameTranslationKey: 'organization_menu_profile',
    },
    {
      link: LINKS.ORGANIZATION_MEMBERS.build({
        [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
      }),
      nameTranslationKey: 'organization_menu_members',
    },
    {
      link: LINKS.ORGANIZATION_MEMBER_PRIVILEGES.build({
        [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
      }),
      nameTranslationKey: 'organization_menu_member_privileges',
    },
    {
      link: LINKS.ORGANIZATION_INVITATIONS.build({
        [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
      }),
      nameTranslationKey: 'organization_menu_invitations',
    },
    {
      link: LINKS.ORGANIZATION_BILLING.build({
        [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
      }),
      nameTranslationKey: 'organization_menu_billing',
    },
  ].map(
    (i) =>
      new OrganizationMenuItem(
        i.link,
        i.nameTranslationKey,
        match.url === i.link
      )
  );
};
