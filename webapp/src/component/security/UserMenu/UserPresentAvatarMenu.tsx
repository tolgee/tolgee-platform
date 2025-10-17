import React, { useState } from 'react';
import { IconButton, MenuItem, Popover, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Link, useHistory, useLocation } from 'react-router-dom';

import {
  useIsAdminOrSupporter,
  useIsSsoMigrationRequired,
  usePreferredOrganization,
  useUser,
} from 'tg.globalContext/helpers';
import { UserAvatar } from 'tg.component/common/avatar/UserAvatar';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';

import { MenuHeader } from './MenuHeader';
import { OrganizationSwitch } from './OrganizationSwitch';
import { ThemeItem } from './ThemeItem';
import { LanguageItem } from './LanguageItem';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { UserMenuItems } from './UserMenuItems';
import { billingMenuItems } from 'tg.ee';

type OrganizationModel = components['schemas']['OrganizationModel'];

const StyledIconButton = styled(IconButton)`
  width: 40px;
  height: 40px;
`;

const StyledPopover = styled(Popover)`
  & .paper {
    margin-top: 5px;
    padding: 2px 0px;
  }
`;

const StyledDivider = styled('div')`
  height: 1px;
  background: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.divider1
      : theme.palette.emphasis[400]};
`;

export const UserPresentAvatarMenu: React.FC = () => {
  const { logout } = useGlobalActions();
  const { preferredOrganization, updatePreferredOrganization } =
    usePreferredOrganization();
  const { t } = useTranslate();
  const location = useLocation();
  const history = useHistory();
  const [anchorEl, setAnchorEl] = useState(null);
  const user = useUser()!;
  const isAdminOrSupporter = useIsAdminOrSupporter();

  const isSsoMigrationRequired = useIsSsoMigrationRequired();

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    //@ts-ignore
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleSelectOrganization = async (organization: OrganizationModel) => {
    setAnchorEl(null);
    await updatePreferredOrganization(organization.id);
    history.push(LINKS.PROJECTS.build());
  };

  const handleCreateNewOrganization = () => {
    setAnchorEl(null);
    history.push(LINKS.ORGANIZATIONS_ADD.build());
  };

  const getOrganizationMenuItems = () =>
    [
      {
        link: LINKS.ORGANIZATION_PROFILE.build({
          [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug || '',
        }),
        label: t('user_menu_organization_settings'),
      },
    ].map((i) => ({
      ...i,
      isSelected: location.pathname === i.link,
    }));

  return (
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
        <UserMenuItems onClose={handleClose} />
        {!isSsoMigrationRequired && preferredOrganization && (
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

            {billingMenuItems.map((Item, index) => (
              <Item key={index} onClose={handleClose} />
            ))}

            <OrganizationSwitch
              onSelect={handleSelectOrganization}
              onCreateNew={handleCreateNewOrganization}
            />
          </>
        )}
        <StyledDivider />
        <LanguageItem />
        <ThemeItem />
        <StyledDivider />

        {isAdminOrSupporter && (
          <MenuItem
            component={Link}
            to={LINKS.ADMINISTRATION_ORGANIZATIONS.build()}
            data-cy="user-menu-server-administration"
          >
            <T keyName="user_menu_server_administration" />
          </MenuItem>
        )}

        <MenuItem onClick={() => logout()} data-cy="user-menu-logout">
          <T keyName="user_menu_logout" />
        </MenuItem>
      </StyledPopover>
    </div>
  );
};
