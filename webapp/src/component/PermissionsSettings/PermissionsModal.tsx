import { useState } from 'react';
import { T } from '@tolgee/react';
import { Button, Dialog, DialogActions, DialogContent } from '@mui/material';

import {
  LanguageModel,
  PermissionModel,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { PermissionsSettings } from 'tg.component/PermissionsSettings/PermissionsSettings';

type Props = {
  allLangs?: LanguageModel[];
  onClose: () => void;
  title: string;
  permissions: PermissionModel;
  onSubmit: (settings: PermissionSettingsState) => Promise<void>;
};

export const PermissionsModal: React.FC<Props> = ({
  allLangs,
  onClose,
  title,
  permissions,
  onSubmit,
}) => {
  const [loading, setLoading] = useState(false);

  const [settingsState, setSettingsState] = useState<
    PermissionSettingsState | undefined
  >(undefined);

  const handleUpdatePermissions = async () => {
    if (settingsState) {
      setLoading(true);
      onSubmit(settingsState).finally(() => {
        onClose();
        setLoading(false);
      });
    }
  };

  return (
    <Dialog open={true} onClose={onClose} fullWidth>
      <DialogContent sx={{ minHeight: 400 }}>
        <PermissionsSettings
          title={title}
          permissions={permissions}
          onChange={setSettingsState}
          allLangs={allLangs}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>
          <T keyName="permission_dialog_close" />
        </Button>
        <LoadingButton
          loading={loading}
          onClick={handleUpdatePermissions}
          color="primary"
          variant="contained"
        >
          <T keyName="permission_dialog_save" />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
