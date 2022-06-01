import { useRef, useState } from 'react';
import { Box, Link, MenuItem, Popover, styled } from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { Link as RouterLink } from 'react-router-dom';

import { components } from 'tg.service/apiSchema.generated';
import { LINKS, PARAMS, Link as UrlLink } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';

type OrganizationModel = components['schemas']['OrganizationModel'];

const StyledOrgItem = styled('div')`
  display: grid;
  grid-auto-flow: column;
  gap: 6px;
  align-items: center;
`;

type Props = {
  selectedId?: number;
  link: UrlLink;
};

const UserOrganizationSettingsSubtitleLink: React.FC<Props> = ({
  selectedId,
  link,
}) => {
  const anchorEl = useRef<HTMLAnchorElement>(null);
  const [isOpen, setIsOpen] = useState(false);

  const handleClose = () => {
    setIsOpen(false);
  };

  const handleClick = () => {
    setIsOpen(true);
  };

  const organizationsLoadable = useApiQuery({
    url: '/v2/organizations',
    method: 'get',
    query: {
      params: { filterCurrentUserOwner: false },
      size: 1000,
    },
  });

  const OrganizationItem = ({ data }: { data: OrganizationModel }) => {
    return (
      <StyledOrgItem>
        <Box>
          <AvatarImg
            key={0}
            owner={{
              name: data.name,
              avatar: data.avatar,
              type: 'ORG',
              id: data.id,
            }}
            size={18}
          />
        </Box>
        <Box>{data.name}</Box>
      </StyledOrgItem>
    );
  };

  const selected = organizationsLoadable.data?._embedded?.organizations?.find(
    (org) => org.id === selectedId
  );

  const MenuItems = () => {
    return (
      <>
        {organizationsLoadable.data?._embedded?.organizations?.map(
          (item, idx) => (
            <MenuItem
              key={idx}
              component={RouterLink}
              to={link.build({
                [PARAMS.ORGANIZATION_SLUG]: item.slug,
              })}
              onClick={() => handleClose()}
            >
              <OrganizationItem data={item} />
            </MenuItem>
          )
        )}
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
          {selected && <OrganizationItem data={selected!} />}
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
