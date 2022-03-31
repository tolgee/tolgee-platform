import React, { ComponentProps, FunctionComponent } from 'react';
import {
  Button,
  ListItemText,
  makeStyles,
  Menu,
  MenuItem,
  Tooltip,
} from '@material-ui/core';
import { ArrowDropDown } from '@material-ui/icons';
import { T, useTranslate } from '@tolgee/react';

import { ProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { components } from 'tg.service/apiSchema.generated';
import { ProjectPermissionType } from 'tg.service/response.types';
import { projectPermissionTypes } from 'tg.constants/projectPermissionTypes';

type PermissionType = NonNullable<
  components['schemas']['ProjectModel']['computedPermissions']['type']
>;

const useStyles = makeStyles((theme) => ({
  item: {
    maxWidth: 300,
  },
  textSecondary: {
    whiteSpace: 'normal',
  },
}));

export const PermissionsMenu: FunctionComponent<{
  title?: string;
  selected: PermissionType;
  onSelect: (value: PermissionType) => void;
  buttonProps?: ComponentProps<typeof Button>;
  minPermissions?: PermissionType;
}> = (props) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const classes = useStyles();
  const t = useTranslate();

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  let types = Object.keys(ProjectPermissionType);

  if (props.minPermissions) {
    types = types.filter((k) =>
      new ProjectPermissions(k as any, undefined).satisfiesPermission(
        props.minPermissions as any
      )
    );
  }

  return (
    <>
      <Tooltip
        title={
          props.title ||
          t(`permission_type_${projectPermissionTypes[props.selected]}_hint`)
        }
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
            <T>{`permission_type_${props.selected.toLowerCase()}`}</T>{' '}
            <ArrowDropDown fontSize="small" />
          </Button>
        </span>
      </Tooltip>
      <Menu
        data-cy="permissions-menu"
        elevation={1}
        id="simple-menu"
        anchorEl={anchorEl}
        getContentAnchorEl={null}
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
            <ListItemText
              className={classes.item}
              primary={<T>{`permission_type_${k.toLowerCase()}`}</T>}
              secondary={<T>{`permission_type_${k.toLowerCase()}_hint`}</T>}
              secondaryTypographyProps={{ className: classes.textSecondary }}
            />
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};
