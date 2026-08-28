import { useState, useRef } from 'react';
import { T } from '@tolgee/react';
import { MenuItem, ListItemText } from '@mui/material';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';
import { OrganizationPopover } from 'tg.component/organizationSwitch/OrganizationPopover';
import { useOrganizationSwitchShape } from 'tg.component/organizationSwitch/useOrganizationSwitchShape';

type OrganizationModel = components['schemas']['OrganizationModel'];

type Props = {
  onSelect: (organization: OrganizationModel) => void;
  onCreateNew: () => void;
  onCommunity: () => void;
};

export const OrganizationSwitch: React.FC<React.PropsWithChildren<Props>> = ({
  onSelect,
  onCreateNew,
  onCommunity,
}) => {
  const anchorEl = useRef<any>(null);
  const [isOpen, setIsOpen] = useState(false);
  const { preferredOrganization } = usePreferredOrganization();
  const { offersCommunityFooter } = useOrganizationSwitchShape();

  const handleClose = () => setIsOpen(false);

  const handleSelectOrganization = (organization: OrganizationModel) => {
    handleClose();
    onSelect(organization);
  };

  const handleCreateNewOrg = () => {
    handleClose();
    onCreateNew();
  };

  const handleCommunity = () => {
    handleClose();
    onCommunity();
  };

  const handleMenuClick = () => setIsOpen((open) => !open);

  if (!preferredOrganization) {
    return null;
  }

  return (
    <>
      <MenuItem
        onClick={handleMenuClick}
        data-cy="user-menu-organization-switch"
        ref={anchorEl}
      >
        <ListItemText sx={{ flexGrow: 1 }}>
          <T
            keyName="user_menu_organization_switch"
            defaultValue="Switch organization"
          />
        </ListItemText>
        <ArrowDropDown />
      </MenuItem>
      <OrganizationPopover
        open={isOpen}
        onClose={handleClose}
        selectedId={preferredOrganization?.id}
        anchorEl={anchorEl.current}
        onSelect={handleSelectOrganization}
        onAddNew={handleCreateNewOrg}
        offersCommunityFooter={offersCommunityFooter}
        onCommunity={handleCommunity}
      />
    </>
  );
};
