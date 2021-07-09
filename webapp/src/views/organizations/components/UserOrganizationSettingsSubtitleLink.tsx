import React from 'react';
import { Box, Link, MenuItem, Popover } from '@material-ui/core';
import { ArrowDropDown } from '@material-ui/icons';
import { T } from '@tolgee/react';
import { Link as RouterLink } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useUser } from 'tg.hooks/useUser';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type UserOrganizationSettingsSubtitleLinkProps = {
  isUser: boolean;
};

type ListDataType = {
  name: string;
  linkTo: string;
}[];

const UserOrganizationSettingsSubtitleLink = (
  props: UserOrganizationSettingsSubtitleLinkProps
) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const isOpen = !!anchorEl;

  const MenuItems = () => {
    const user = useUser();

    const organizationsLoadable = useApiQuery({
      url: '/v2/organizations',
      method: 'get',
      query: {
        params: { filterCurrentUserOwner: false },
        size: 1000,
      },
    });

    const data: ListDataType = [
      {
        name: user?.name as string,
        linkTo: LINKS.USER_SETTINGS.build(),
      },
    ];

    organizationsLoadable.data?._embedded?.organizations?.map((i) => {
      data.push({
        name: i.name,
        linkTo: LINKS.ORGANIZATION_PROFILE.build({
          [PARAMS.ORGANIZATION_SLUG]: i.slug,
        }),
      });
    });

    return (
      <>
        {data.map((item, idx) => (
          <MenuItem
            key={idx}
            component={RouterLink}
            to={item.linkTo}
            onClick={() => handleClose()}
          >
            {item.name}
          </MenuItem>
        ))}
      </>
    );
  };

  return (
    <>
      <Box display="flex" data-cy="user-organizations-settings-subtitle-link">
        <Link
          style={{
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            flexWrap: 'wrap',
            flexShrink: 1,
          }}
          onClick={handleClick}
        >
          {props.isUser ? (
            <T>user_account_subtitle</T>
          ) : (
            <T>organization_account_subtitle</T>
          )}
          <ArrowDropDown fontSize={'small'} />
        </Link>

        <Popover
          elevation={1}
          id="simple-menu"
          anchorEl={anchorEl}
          getContentAnchorEl={null}
          keepMounted
          open={Boolean(anchorEl)}
          onClose={handleClose}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'center',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
        >
          {isOpen && <MenuItems />}
        </Popover>
      </Box>
    </>
  );
};

export default UserOrganizationSettingsSubtitleLink;
