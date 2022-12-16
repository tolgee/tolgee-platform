import React, { ComponentProps, FunctionComponent } from 'react';
import {
  Button,
  ListItemText,
  Menu,
  MenuItem,
  styled,
  Tooltip,
} from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { T, useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { ProjectPermissionType } from 'tg.service/response.types';
import { AdvancedPermissionsMenuItem } from './AdvancedPermissionsMenuItem';

type PermissionType = NonNullable<
  components['schemas']['PermissionModel']['type']
>;

const StyledListItemText = styled(ListItemText)`
  max-width: 300px;

  & .textSecondary {
    white-space: normal;
  }
`;

// todo: create separate advanced component
export const PermissionsMenu: FunctionComponent<{
  buttonTooltip?: string;
  selected?: PermissionType;
  onSelect: (value: PermissionType) => void;
  buttonProps?: ComponentProps<typeof Button>;
  minPermissions?: PermissionType;
  user: { name?: string; id: number };
  enableAdvanced?: boolean;
}> = (props) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const t = useTranslate();

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const types = Object.keys(ProjectPermissionType);

  const selected = props.selected?.toLowerCase() ?? 'organization_base';

  return (
    <>
      <Tooltip
        title={props.buttonTooltip || t(`permission_type_${selected}_hint`)}
      >
        <span>
          <Button
            data-cy="permissions-menu-button"
            {...props.buttonProps}
            variant="outlined"
            aria-controls="simple-menu"
            aria-haspopup="true"
            onClick={handleClick}
          >
            <T>{`permission_type_${selected}`}</T>{' '}
            <ArrowDropDown fontSize="small" />
          </Button>
        </span>
      </Tooltip>
      <Menu
        data-cy="permissions-menu"
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
        {types.map((k) => (
          <MenuItem
            key={k}
            onClick={() => {
              props.onSelect(k as any);
              handleClose();
            }}
            disabled={k === props.selected}
            selected={k === props.selected}
          >
            <StyledListItemText
              primary={<T>{`permission_type_${k.toLowerCase()}`}</T>}
              secondary={<T>{`permission_type_${k.toLowerCase()}_hint`}</T>}
              secondaryTypographyProps={{ className: 'textSecondary' }}
            />
          </MenuItem>
        ))}
        {props.enableAdvanced && (
          <AdvancedPermissionsMenuItem user={props.user} />
        )}
      </Menu>
    </>
  );
};
