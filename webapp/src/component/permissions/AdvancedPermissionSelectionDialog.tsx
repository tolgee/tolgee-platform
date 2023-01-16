import { Dialog, DialogContent, DialogTitle } from '@mui/material';
import { T } from '@tolgee/react';
import { default as React, FC, useState } from 'react';
import { ScopeSelector } from './ScopeSelector';
import { PermissionsMenu } from './PermissionsMenu';
import { components } from 'tg.service/apiSchema.generated';

type PermissionType = NonNullable<
  components['schemas']['PermissionModel']['type']
>;

export type SimpleUser = { username: string; id: number; name: string };

export const AdvancedPermissionSelectionDialog: FC<{
  onClose: () => void;
  open: boolean;
  user: SimpleUser;
}> = (props) => {
  const [selectedType, setSelectedType] = useState('NONE' as PermissionType);

  return (
    <Dialog
      open={props.open}
      onClose={props.onClose}
      fullWidth
      maxWidth={'md'}
      data-cy="permissions-edit-advanced-dialog"
    >
      <DialogTitle>
        <T
          params={{ userName: props.user.name! }}
          keyName="advanced_permissions_dialog_title"
        />
      </DialogTitle>
      <DialogContent>
        <PermissionsMenu
          onSelect={setSelectedType}
          selected={selectedType}
          user={props.user}
        />
        <ScopeSelector />
      </DialogContent>
    </Dialog>
  );
};
