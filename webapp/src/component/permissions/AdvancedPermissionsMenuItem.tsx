import { ListItemText, MenuItem } from '@mui/material';
import { T } from '@tolgee/react';
import React, { FC, useState } from 'react';
import { AdvancedPermissionSelectionDialog } from './AdvancedPermissionSelectionDialog';

export const AdvancedPermissionsMenuItem: FC<{
  user: { name?: string; id: number };
}> = (props) => {
  const [open, setOpen] = useState(false);

  return (
    <>
      <AdvancedPermissionSelectionDialog
        onClose={() => setOpen(false)}
        user={props.user}
        open={open}
      />
      <MenuItem onClick={() => setOpen(true)}>
        <ListItemText>
          <T>{`advanced_permissions_menu_item`}</T>
        </ListItemText>
      </MenuItem>
    </>
  );
};
