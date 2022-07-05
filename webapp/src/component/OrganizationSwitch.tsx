import { useRef, useState } from 'react';
import { Box, Link, MenuItem, Popover, styled } from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import {
  useCurrentOrganization,
  useUpdateCurrentOrganization,
} from 'tg.hooks/CurrentOrganizationProvider';

type OrganizationModel = components['schemas']['OrganizationModel'];

const StyledOrgItem = styled('div')`
  display: grid;
  grid-auto-flow: column;
  gap: 6px;
  align-items: center;
`;

type Props = {
  onSelect?: (organization: OrganizationModel) => void;
  ownedOnly?: boolean;
};

export const OrganizationSwitch: React.FC<Props> = ({
  onSelect,
  ownedOnly,
}) => {
  const anchorEl = useRef<HTMLAnchorElement>(null);
  const [isOpen, setIsOpen] = useState(false);
  const organization = useCurrentOrganization();
  const updateCurrentOrganization = useUpdateCurrentOrganization();
  const history = useHistory();

  const handleClose = () => {
    setIsOpen(false);
  };

  const handleClick = () => {
    setIsOpen(true);
  };

  const handleSelectOrganization = (organization: OrganizationModel) => {
    handleClose();
    updateCurrentOrganization(organization);
    onSelect?.(organization);
  };

  const handleCreateNewOrg = () => {
    handleClose();
    history.push(LINKS.ORGANIZATIONS_ADD.build());
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
    (org) => org.id === organization.id
  );

  const MenuItems = () => {
    return (
      <>
        {organizationsLoadable.data?._embedded?.organizations
          ?.filter((org) =>
            ownedOnly ? org.currentUserRole === 'OWNER' : true
          )
          ?.map((item, idx) => (
            <MenuItem
              key={idx}
              selected={item.id === selected?.id}
              onClick={() => handleSelectOrganization(item)}
            >
              <OrganizationItem data={item} />
            </MenuItem>
          ))}
      </>
    );
  };

  return (
    <>
      <Box
        display="flex"
        data-cy="user-organizations-settings-subtitle-link"
        mr={-1}
      >
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
            horizontal: 'center',
          }}
        >
          <MenuItems />
          <MenuItem onClick={handleCreateNewOrg}>
            <T keyName="organizations_add_new" />
          </MenuItem>
        </Popover>
      </Box>
    </>
  );
};
