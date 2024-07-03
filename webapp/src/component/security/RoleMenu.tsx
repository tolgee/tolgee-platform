import { ComponentProps, useState } from 'react';
import {
  Button,
  ListItemText,
  Menu,
  MenuItem,
  styled,
  Tooltip,
} from '@mui/material';
import { ArrowDropDown } from 'tg.component/CustomIcons';

import { OrganizationRoleType } from 'tg.service/response.types';
import { components } from 'tg.service/apiSchema.generated';
import { useOrgRoleTranslation } from 'tg.translationTools/useOrgRoleTranslation';

type RoleType =
  components['schemas']['UserAccountWithOrganizationRoleModel']['organizationRole'];

const StyledListItemText = styled(ListItemText)`
  max-width: 300px;
  & .textSecondary {
    white-space: normal;
  }
`;

type Props = {
  buttonProps?: ComponentProps<typeof Button>;
  role: RoleType;
  onSelect: (value: RoleType) => void;
};

export const RoleMenu: React.FC<Props> = (props) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const translateRole = useOrgRoleTranslation();

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  return (
    <>
      <Tooltip title={translateRole(props.role!, true)}>
        <span>
          <Button
            data-cy="organization-role-menu-button"
            {...props.buttonProps}
            variant="outlined"
            size="small"
            aria-controls="simple-menu"
            aria-haspopup="true"
            onClick={handleClick}
          >
            {translateRole(props.role!)} <ArrowDropDown fontSize="small" />
          </Button>
        </span>
      </Tooltip>
      <Menu
        data-cy="organization-role-menu"
        elevation={1}
        id="simple-menu"
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
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
        {Object.keys(OrganizationRoleType).map((k) => (
          <MenuItem
            key={k}
            onClick={() => {
              props.onSelect(k as any);
              handleClose();
            }}
            selected={k === props.role}
          >
            <StyledListItemText
              data-cy="organization-role-select-item"
              primary={translateRole(k)}
              secondary={translateRole(k, true)}
              secondaryTypographyProps={{ className: 'textSecondary' }}
            />
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};
