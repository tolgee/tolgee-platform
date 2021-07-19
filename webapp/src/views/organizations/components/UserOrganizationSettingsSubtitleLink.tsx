import React, { useMemo, useRef, useState } from 'react';
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
  const anchorEl = useRef<HTMLDivElement>(null);
  const [isOpen, setIsOpen] = useState(false);

  const handleClose = () => {
    setIsOpen(false);
  };

  const handleClick = () => {
    setIsOpen(true);
  };

  const user = useUser();

  const organizationsLoadable = useApiQuery({
    url: '/v2/organizations',
    method: 'get',
    query: {
      params: { filterCurrentUserOwner: false },
      size: 1000,
    },
  });

  const data: ListDataType = useMemo(
    () => [
      {
        name: user?.name as string,
        linkTo: LINKS.USER_SETTINGS.build(),
      },
      ...(organizationsLoadable.data?._embedded?.organizations?.map((i) => ({
        name: i.name,
        linkTo: LINKS.ORGANIZATION_PROFILE.build({
          [PARAMS.ORGANIZATION_SLUG]: i.slug,
        }),
      })) || []),
    ],
    [organizationsLoadable.data]
  );

  const MenuItems = () => {
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
          ref={anchorEl}
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
          anchorEl={anchorEl.current}
          keepMounted
          open={isOpen}
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
