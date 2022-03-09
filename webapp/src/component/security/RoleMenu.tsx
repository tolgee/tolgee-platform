import { ComponentProps, useState } from 'react';
import {
  Button,
  ListItemText,
  Menu,
  MenuItem,
  Tooltip,
} from '@material-ui/core';
import { ArrowDropDown } from '@material-ui/icons';
import { T, useTranslate } from '@tolgee/react';

import { OrganizationRoleType } from 'tg.service/response.types';
import { components } from 'tg.service/apiSchema.generated';
import { makeStyles } from '@material-ui/core';

type RoleType =
  components['schemas']['UserAccountWithOrganizationRoleModel']['organizationRole'];

const useStyles = makeStyles((theme) => ({
  item: {
    maxWidth: 300,
  },
  textSecondary: {
    whiteSpace: 'normal',
  },
}));

type Props = {
  buttonProps?: ComponentProps<typeof Button>;
  role: RoleType;
  onSelect: (value: RoleType) => void;
};

export const RoleMenu: React.FC<Props> = (props) => {
  const classes = useStyles();
  const t = useTranslate();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  return (
    <>
      <Tooltip title={t(`organization_role_type_${props.role}_hint`)}>
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
            <T>{`organization_role_type_${props.role}`}</T>{' '}
            <ArrowDropDown fontSize="small" />
          </Button>
        </span>
      </Tooltip>
      <Menu
        data-cy="organization-role-menu"
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
        {Object.keys(OrganizationRoleType).map((k) => (
          <MenuItem
            key={k}
            onClick={() => {
              props.onSelect(k as any);
              handleClose();
            }}
            selected={k === props.role}
          >
            <ListItemText
              data-cy="organization-role-select-item"
              className={classes.item}
              primary={<T>{`organization_role_type_${k}`}</T>}
              secondary={<T>{`organization_role_type_${k}_hint`}</T>}
              secondaryTypographyProps={{ className: classes.textSecondary }}
            />
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};
