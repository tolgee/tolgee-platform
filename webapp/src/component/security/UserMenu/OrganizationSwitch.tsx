import { useState } from 'react';
import { T } from '@tolgee/react';
import { MenuItem, ListItemText, Menu, styled } from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useCurrentOrganization } from 'tg.hooks/InitialDataProvider';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { components } from 'tg.service/apiSchema.generated';

type OrganizationModel = components['schemas']['OrganizationModel'];

const StyledMenuItem = styled(MenuItem)`
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px;
`;

type Props = {
  onSelect: (organization: OrganizationModel) => void;
  onCreateNew: () => void;
};

export const OrganizationSwitch: React.FC<Props> = ({
  onSelect,
  onCreateNew,
}) => {
  const [menuOpen, setMenuOpen] = useState<HTMLElement | null>(null);
  const organization = useCurrentOrganization();

  const setOrganization = (organization: OrganizationModel) => () => {
    setMenuOpen(null);
    onSelect(organization);
  };

  const handleCreateNewOrg = () => {
    setMenuOpen(null);
    onCreateNew();
  };

  const handleMenuClick = (e) => {
    if (menuOpen) {
      setMenuOpen(null);
    } else {
      setMenuOpen(e.currentTarget);
    }
  };

  const organizationsLoadable = useApiQuery({
    url: '/v2/organizations',
    method: 'get',
    query: {
      params: { filterCurrentUserOwner: false },
      size: 1000,
    },
  });

  return (
    <>
      <MenuItem onClick={handleMenuClick}>
        <ListItemText sx={{ flexGrow: 1 }}>
          <T keyName="user_menu_organization_switch">Switch organization</T>
        </ListItemText>
        <ArrowDropDown />
      </MenuItem>
      <Menu
        anchorOrigin={{
          vertical: 'top',
          horizontal: 'center',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'center',
        }}
        PaperProps={{
          sx: { minWidth: menuOpen?.clientWidth },
        }}
        open={Boolean(menuOpen)}
        anchorEl={menuOpen}
        onClose={() => setMenuOpen(null)}
      >
        {organizationsLoadable.data?._embedded?.organizations?.map((item) => {
          return (
            <StyledMenuItem
              key={item.slug}
              value={item.slug}
              onClick={setOrganization(item)}
              selected={item.id === organization?.id}
            >
              <AvatarImg
                owner={{
                  avatar: item.avatar,
                  id: item.id,
                  name: item.name,
                  type: 'ORG',
                }}
                size={26}
              />
              <ListItemText primary={item.name} />
            </StyledMenuItem>
          );
        })}
        <StyledMenuItem onClick={handleCreateNewOrg}>
          <T keyName="organizations_add_new" />
        </StyledMenuItem>
      </Menu>
    </>
  );
};
