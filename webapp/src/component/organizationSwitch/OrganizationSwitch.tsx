import { useRef, useState } from 'react';
import { Box, Link, styled } from '@mui/material';
import { ArrowDropDown } from 'tg.component/CustomIcons';

import { components } from 'tg.service/apiSchema.generated';
import { OrganizationItem } from './OrganizationItem';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { OrganizationPopover } from './OrganizationPopover';

type OrganizationModel = components['schemas']['OrganizationModel'];

const StyledLink = styled(Link)`
  display: flex;
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

  const handleSelectOrganization = async (organization: OrganizationModel) => {
    handleClose();
    await updatePreferredOrganization(organization.id);
    onSelect?.(organization);
  };

  const handleCreateNewOrg = () => {
    handleClose();
    history.push(LINKS.ORGANIZATIONS_ADD.build());
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
          {preferredOrganization && (
            <OrganizationItem data={preferredOrganization} />
          )}
          <ArrowDropDown
            width={20}
            height={20}
            style={{ marginRight: '-6px' }}
          />
        </StyledLink>

        <OrganizationPopover
          ownedOnly={ownedOnly}
          open={isOpen}
          onClose={handleClose}
          selected={preferredOrganization}
          onSelect={handleSelectOrganization}
          anchorEl={anchorEl.current!}
          onAddNew={handleCreateNewOrg}
        />
      </Box>
    </>
  );
};
