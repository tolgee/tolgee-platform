import { useState, useRef } from 'react';
import { T } from '@tolgee/react';
import { MenuItem, ListItemText } from '@mui/material';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';
import { OrganizationPopover } from 'tg.component/organizationSwitch/OrganizationPopover';

type OrganizationModel = components['schemas']['OrganizationModel'];

type Props = {
  onSelect: (organization: OrganizationModel) => void;
  onCreateNew: () => void;
};

export const OrganizationSwitch: React.FC<Props> = ({
  onSelect,
  onCreateNew,
}) => {
  const anchorEl = useRef<any>(null);
  const [isOpen, setIsOpen] = useState(false);
  const { preferredOrganization } = usePreferredOrganization();

  const handleClose = () => setIsOpen(false);

  const handleSelectOrganization = (organization: OrganizationModel) => {
    setIsOpen(false);
    onSelect(organization);
  };

  const handleCreateNewOrg = () => {
    setIsOpen(false);
    onCreateNew();
  };

  const handleMenuClick = (e) => {
    if (isOpen) {
      setIsOpen(false);
    } else {
      setIsOpen(true);
    }
  };

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
        selected={preferredOrganization}
        anchorEl={anchorEl.current}
        onSelect={handleSelectOrganization}
        onAddNew={handleCreateNewOrg}
      />
    </>
  );
};
