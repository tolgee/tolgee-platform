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
import { useOrganizationSwitchShape } from 'tg.component/organizationSwitch/useOrganizationSwitchShape';

type OrganizationModel = components['schemas']['OrganizationModel'];

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
  plain?: boolean;
};

export const OrganizationSwitch: React.FC<React.PropsWithChildren<Props>> = ({
  onSelect,
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
    if (await updatePreferredOrganization(organization.id)) {
      onSelect?.(organization);
    }
  };

  const handleCreateNewOrg = () => {
    handleClose();
    history.push(LINKS.ORGANIZATIONS_ADD.build());
  };

  const handleCommunity = () => {
    handleClose();
    history.push(LINKS.COMMUNITY_PROJECTS.build());
  };

  const shape = useOrganizationSwitchShape();

  if (shape.render === 'hidden') {
    return null;
  }

  if (shape.render === 'community-label') {
    return (
      <Box display="flex" overflow="hidden">
        <CommunityTranslationItem />
      </Box>
    );
  }

  const switchLabel =
    // `&& preferredOrganization` narrows for the compiler; the rule itself lives in
    // organizationSwitchShape, which only answers 'organization' when there is one.
    shape.label === 'organization' && preferredOrganization ? (
      <OrganizationItem data={preferredOrganization} />
    ) : (
      <CommunityTranslationItem />
    );

  return (
    <Box display="flex" data-cy="organization-switch" overflow="hidden">
      <StyledLink plain={plain} ref={anchorEl} onClick={handleClick}>
        {switchLabel}
        <ArrowDropDown width={20} height={20} style={{ marginRight: '-6px' }} />
      </StyledLink>

      <OrganizationPopover
        open={isOpen}
        onClose={handleClose}
        selectedId={
          shape.communitySelected ? undefined : preferredOrganization?.id
        }
        onSelect={handleSelectOrganization}
        anchorEl={anchorEl.current!}
        onAddNew={handleCreateNewOrg}
        offersCommunityFooter={shape.offersCommunityFooter}
        onCommunity={handleCommunity}
      />
    </Box>
  );
};
