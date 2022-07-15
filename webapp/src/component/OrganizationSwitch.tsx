import { useRef, useState } from 'react';
import { Box, Link, MenuItem, Popover, styled } from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';

type OrganizationModel = components['schemas']['OrganizationModel'];

const StyledLink = styled(Link)`
  display: flex;
`;

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
  const { preferredOrganization } = usePreferredOrganization();
  const { updatePreferredOrganization } = usePreferredOrganization();
  const history = useHistory();

  const handleClose = () => {
    setIsOpen(false);
  };

  const handleClick = () => {
    setIsOpen(true);
  };

  const handleSelectOrganization = (organization: OrganizationModel) => {
    handleClose();
    updatePreferredOrganization(organization);
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
      sort: ['name'],
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
    (org) => org.id === preferredOrganization.id
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
              data-cy="organization-switch-item"
            >
              <OrganizationItem data={item} />
            </MenuItem>
          ))}
      </>
    );
  };

  return (
    <>
      <Box display="flex" data-cy="organization-switch" overflow="hidden">
        <StyledLink
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
          {selected && <OrganizationItem data={selected} />}
          <ArrowDropDown fontSize={'small'} sx={{ marginRight: '-6px' }} />
        </StyledLink>

        <Popover
          elevation={1}
          id="simple-menu"
          anchorEl={anchorEl.current}
          keepMounted
          open={isOpen}
          onClose={handleClose}
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'center',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'center',
          }}
        >
          <MenuItems />
          <MenuItem
            onClick={handleCreateNewOrg}
            data-cy="organization-switch-new"
          >
            <T keyName="organizations_add_new" />
          </MenuItem>
        </Popover>
      </Box>
    </>
  );
};
