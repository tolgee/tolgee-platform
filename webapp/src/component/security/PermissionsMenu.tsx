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

import { ProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { components } from 'tg.service/apiSchema.generated';
import { ProjectPermissionType } from 'tg.service/response.types';
import { usePermissionTranslation } from 'tg.translationTools/usePermissionTranslation';

type PermissionType = NonNullable<
  components['schemas']['ProjectModel']['computedPermissions']['type']
>;

const StyledListItemText = styled(ListItemText)`
  max-width: 300px;
  & .textSecondary {
    white-space: normal;
  }
`;

export const PermissionsMenu: FunctionComponent<{
  title?: string;
  selected: PermissionType;
  onSelect: (value: PermissionType) => void;
  buttonProps?: ComponentProps<typeof Button>;
  minPermissions?: PermissionType;
}> = (props) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  let types = Object.keys(ProjectPermissionType);

  const translatePermission = usePermissionTranslation();

  if (props.minPermissions) {
    types = types.filter((k) =>
      new ProjectPermissions(k as any, undefined, false).satisfiesPermission(
        props.minPermissions as any
      )
    );
  }

  return (
    <>
      <Tooltip
        title={props.title || translatePermission(props.selected, true)}
        disableInteractive
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
            {translatePermission(props.selected)}{' '}
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
              primary={translatePermission(k)}
              secondary={translatePermission(k, true)}
              secondaryTypographyProps={{ className: 'textSecondary' }}
            />
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};
