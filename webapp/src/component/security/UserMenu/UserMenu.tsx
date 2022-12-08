import React, { useState } from 'react';
import { IconButton, MenuItem, Popover, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useSelector } from 'react-redux';
import { Link, useHistory, useLocation } from 'react-router-dom';

import {
  useConfig,
  useOrganizationUsage,
  usePreferredOrganization,
  useUser,
} from 'tg.globalContext/helpers';
import { useUserMenuItems } from 'tg.hooks/useUserMenuItems';
import { AppState } from 'tg.store/index';
import { UserAvatar } from 'tg.component/common/avatar/UserAvatar';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { getProgressData } from 'tg.component/billing/utils';

import { MenuHeader } from './MenuHeader';
import { OrganizationSwitch } from './OrganizationSwitch';
import { BillingItem } from './BillingItem';
import { useLogout } from 'tg.hooks/useLogout';

type OrganizationModel = components['schemas']['OrganizationModel'];

const StyledIconButton = styled(IconButton)`
  width: 40px;
  height: 40px;
`;

const StyledPopover = styled(Popover)`
  & .paper {
    margin-top: 5px;
    padding: 2px 0px;
    max-width: 300px;
  }
`;

const StyledDivider = styled('div')`
  height: 1px;
  background: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[300]
      : theme.palette.emphasis[400]};
`;

export const UserMenu: React.FC = () => {
  const userLogged = useSelector(
    (state: AppState) => state.global.security.allowPrivate
  );
  const { preferredOrganization, updatePreferredOrganization } =
    usePreferredOrganization();
  const { t } = useTranslate();
  const config = useConfig();
  const location = useLocation();
  const history = useHistory();
  const [anchorEl, setAnchorEl] = useState(null);
  const user = useUser();
  const userMenuItems = useUserMenuItems();
  const { usage } = useOrganizationUsage();
  const progressData = usage && getProgressData(usage);

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    //@ts-ignore
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const logout = useLogout();

  const handleSelectOrganization = (organization: OrganizationModel) => {
    updatePreferredOrganization(organization);
    setAnchorEl(null);
    history.push(LINKS.PROJECTS.build());
  };

  const handleCreateNewOrganization = () => {
    setAnchorEl(null);
    history.push(LINKS.ORGANIZATIONS_ADD.build());
  };

  if (!config.authentication || !user) {
    return null;
  }

  const showBilling =
    config.billing.enabled &&
    preferredOrganization?.currentUserRole === 'OWNER';

  const getOrganizationMenuItems = () =>
    [
      {
        link: LINKS.ORGANIZATION_PROFILE.build({
          [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
        }),
        label: t('user_menu_organization_settings'),
      },
    ].map((i) => ({
      ...i,
      isSelected: location.pathname === i.link,
    }));

  return userLogged ? (
    <div>
      <StyledIconButton
        color="inherit"
        data-cy="global-user-menu-button"
        aria-controls="user-menu"
        aria-haspopup="true"
        onClick={handleOpen}
        size="large"
      >
        <UserAvatar />
      </StyledIconButton>
      <StyledPopover
        id="user-menu"
        keepMounted
        open={!!anchorEl}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        classes={{ paper: 'paper' }}
      >
        <MenuHeader
          entity={user}
          type="USER"
          title={user.name}
          subtitle={user.username}
        />
        {userMenuItems.map((item, index) => (
          <MenuItem
            key={index}
            component={Link}
            to={item.link}
            selected={item.isSelected}
            onClick={handleClose}
            data-cy="user-menu-user-settings"
          >
            {item.label}
          </MenuItem>
        ))}
        {preferredOrganization && (
          <>
            <StyledDivider />
            <MenuHeader
              entity={preferredOrganization}
              type="ORG"
              title={preferredOrganization.name}
              subtitle={preferredOrganization.description}
            />
            {getOrganizationMenuItems().map((item, index) => (
              <MenuItem
                key={index}
                component={Link}
                to={item.link}
                selected={item.isSelected}
                onClick={handleClose}
                data-cy="user-menu-organization-settings"
              >
                {item.label}
              </MenuItem>
            ))}

            {showBilling && (
              <BillingItem
                progressData={progressData}
                onClose={handleClose}
                organizationSlug={preferredOrganization.slug}
              />
            )}

            <OrganizationSwitch
              onSelect={handleSelectOrganization}
              onCreateNew={handleCreateNewOrganization}
            />
          </>
        )}
        <StyledDivider />

        {user.globalServerRole == 'ADMIN' && (
          <MenuItem
            component={Link}
            to={LINKS.ADMINISTRATION_ORGANIZATIONS.build()}
            data-cy="user-menu-server-administration"
          >
            <T>user_menu_server_administration</T>
          </MenuItem>
        )}

        <MenuItem onClick={logout} data-cy="user-menu-logout">
          <T>user_menu_logout</T>
        </MenuItem>
      </StyledPopover>
    </div>
  ) : null;
};
