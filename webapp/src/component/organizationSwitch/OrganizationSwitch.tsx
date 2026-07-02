import { useRef, useState } from 'react';
import { Box, Link, styled } from '@mui/material';
import { ArrowDropDown } from 'tg.component/CustomIcons';

import { components } from 'tg.service/apiSchema.generated';
import { OrganizationItem } from './OrganizationItem';
import { CommunityTranslationItem } from './CommunityTranslationItem';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { OrganizationPopover } from './OrganizationPopover';

type OrganizationModel = components['schemas']['OrganizationModel'];

export type SwitchSurface = 'organization' | 'community';

const StyledLink = styled(Link, {
  shouldForwardProp: (prop) => prop !== 'plain',
})<{ plain?: boolean }>`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  flex-shrink: 1;
  cursor: pointer;
  ${({ plain, theme }) => plain && `color: ${theme.palette.text.primary};`}
`;

type Props = {
  onSelect?: (organization: OrganizationModel) => void;
  ownedOnly?: boolean;
  selectedSurface?: SwitchSurface;
  plain?: boolean;
};

export const OrganizationSwitch: React.FC<Props> = ({
  onSelect,
  ownedOnly,
  selectedSurface = 'organization',
  plain,
}) => {
  const anchorEl = useRef<HTMLAnchorElement>(null);
  const [isOpen, setIsOpen] = useState(false);
  const { preferredOrganization, updatePreferredOrganization } =
    usePreferredOrganization();
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

  // TODO: fully implemented, but after talk with designer we decided to
  // show the community only when user contributed to a public project
  // which needs contributors tracking - will be implemented in future pitch
  // const handleCommunityNavigate = () => {
  //   history.push(LINKS.COMMUNITY_PROJECTS.build());
  // };

  const isCommunitySurface = selectedSurface === 'community';

  const switchLabel = isCommunitySurface ? (
    <CommunityTranslationItem />
  ) : preferredOrganization ? (
    <OrganizationItem data={preferredOrganization} />
  ) : null;

  return (
    <Box display="flex" data-cy="organization-switch" overflow="hidden">
      <StyledLink plain={plain} ref={anchorEl} onClick={handleClick}>
        {switchLabel}
        <ArrowDropDown width={20} height={20} style={{ marginRight: '-6px' }} />
      </StyledLink>

      <OrganizationPopover
        ownedOnly={ownedOnly}
        open={isOpen}
        onClose={handleClose}
        selected={preferredOrganization}
        onSelect={handleSelectOrganization}
        anchorEl={anchorEl.current!}
        onAddNew={handleCreateNewOrg}
        communitySelected={isCommunitySurface}
        // onCommunityNavigate={handleCommunityNavigate}
      />
    </Box>
  );
};
