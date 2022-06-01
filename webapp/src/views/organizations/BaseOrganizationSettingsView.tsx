import { PropsWithChildren } from 'react';
import { Box, Typography, styled } from '@mui/material';
import { useRouteMatch } from 'react-router-dom';

import { BaseView, BaseViewProps } from 'tg.component/layout/BaseView';
import { Link, LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { OrganizationSettingsMenu } from './components/OrganizationSettingsMenu';
import UserOrganizationSettingsSubtitleLink from './components/UserOrganizationSettingsSubtitleLink';
import { Navigation, NavigationItem } from 'tg.component/navigation/Navigation';
import { SecondaryBar } from 'tg.component/layout/SecondaryBar';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useTranslate } from '@tolgee/react';

const StyledHeaderWrapper = styled('div')`
  display: grid;
  grid-auto-flow: column;
  align-items: center;
  margin-top: -4px;
  margin-bottom: -4px;
  height: 24px;
`;

const StyledNavigation = styled('div')``;

const StyledOrganization = styled('div')`
  display: flex;
  justify-self: end;
`;

const StyledContainer = styled('div')`
  display: grid;
  grid-auto-flow: column;
  grid-template-columns: 250px 1fr;
  gap: 32px;
`;

const StyledContent = styled('div')`
  display: grid;
`;

type Props = BaseViewProps & {
  link: Link;
};

export const BaseOrganizationSettingsView: React.FC<Props> = ({
  children,
  title,
  loading,
  navigation,
  link,
  ...otherProps
}) => {
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];
  const t = useTranslate();

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
    <BaseView
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
      hideChildrenOnLoading={false}
    >
      <StyledContainer>
        <Box>
          <OrganizationSettingsMenu />
        </Box>
        <StyledContent>
          {title && (
            <Box mb={2}>
              <Typography variant="h6">{title}</Typography>
            </Box>
          )}
          {children}
        </StyledContent>
      </StyledContainer>
    </BaseView>
  );
};
