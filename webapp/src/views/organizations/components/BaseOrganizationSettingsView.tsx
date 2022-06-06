import { styled } from '@mui/material';
import { useRouteMatch } from 'react-router-dom';

import { BaseViewProps } from 'tg.component/layout/BaseView';
import { Link, LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import UserOrganizationSettingsSubtitleLink from './UserOrganizationSettingsSubtitleLink';
import { Navigation, NavigationItem } from 'tg.component/navigation/Navigation';
import { SecondaryBar } from 'tg.component/layout/SecondaryBar';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useTranslate } from '@tolgee/react';
import { BaseSettingsView } from 'tg.component/layout/BaseSettingsView/BaseSettingsView';
import { SettingsMenuItem } from 'tg.component/layout/BaseSettingsView/SettingsMenu';

const StyledHeaderWrapper = styled('div')`
  display: grid;
  grid-auto-flow: column;
  align-items: start;
  margin-top: -4px;
  margin-bottom: -4px;
  min-height: 24px;
  gap: 10px;
`;

const StyledNavigation = styled('div')``;

const StyledOrganization = styled('div')`
  display: flex;
  justify-self: end;
`;

type Props = BaseViewProps & {
  link: Link;
};

export const BaseOrganizationSettingsView: React.FC<Props> = ({
  children,
  loading,
  navigation,
  link,
  ...otherProps
}) => {
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];
  const t = useTranslate();

  const menuItems: SettingsMenuItem[] = [
    {
      link: LINKS.ORGANIZATION_PROFILE.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_profile'),
    },
    {
      link: LINKS.ORGANIZATION_MEMBERS.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_members'),
    },
    {
      link: LINKS.ORGANIZATION_MEMBER_PRIVILEGES.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_member_privileges'),
    },
    {
      link: LINKS.ORGANIZATION_BILLING.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_billing'),
    },
  ];

  const organization = useApiQuery({
    url: '/v2/organizations/{slug}',
    method: 'get',
    path: { slug: organizationSlug },
  });

  const NavigationItem: NavigationItem[] = organization.data
    ? [
        [t('organizations_settings_title'), LINKS.ORGANIZATIONS.build()],
        [
          organization.data.name,
          LINKS.ORGANIZATION_PROFILE.build({
            [PARAMS.ORGANIZATION_SLUG]: organization.data.slug,
          }),
          <AvatarImg
            key={0}
            owner={{
              name: organization.data.name,
              avatar: organization.data.avatar,
              type: 'ORG',
              id: organization.data.id,
            }}
            size={18}
          />,
        ],
      ]
    : [];

  return (
    <BaseSettingsView
      {...otherProps}
      loading={organization.isLoading || loading}
      customNavigation={
        <SecondaryBar>
          <StyledHeaderWrapper>
            <StyledNavigation>
              <Navigation path={[...NavigationItem, ...(navigation || [])]} />
            </StyledNavigation>
            <StyledOrganization>
              <UserOrganizationSettingsSubtitleLink
                link={link}
                selectedId={organization.data?.id}
              />
            </StyledOrganization>
          </StyledHeaderWrapper>
        </SecondaryBar>
      }
      menuItems={menuItems}
      hideChildrenOnLoading={false}
    >
      {children}
    </BaseSettingsView>
  );
};
