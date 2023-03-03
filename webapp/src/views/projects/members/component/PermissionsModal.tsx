import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Button, Dialog, DialogActions, DialogContent } from '@mui/material';

import {
  PermissionModel,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { PermissionsSettings } from 'tg.component/PermissionsSettings/PermissionsSettings';
import { useProject } from 'tg.hooks/useProject';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useUpdatePermissions } from './useUpdatePermissions';

type Props = {
  onClose: () => void;
  user: { name?: string; id: number };
  permissions: PermissionModel;
};

export const PermissionsModal: React.FC<Props> = ({
  onClose,
  user,
  permissions,
}) => {
  const { t } = useTranslate();
  const messages = useMessage();
  const project = useProject();

  const [settingsState, setSettingsState] = useState<
    PermissionSettingsState | undefined
  >(undefined);

  const afterActions = {
    onSuccess() {
      messages.success(<T>permissions_set_message</T>);
      onClose();
    },
    onError(e) {
      parseErrorResponse(e).forEach((err) => messages.error(<T>{err}</T>));
    },
  };

  const { updatePermissions, isLoading } = useUpdatePermissions({
    userId: user.id,
    projectId: project.id,
    after: afterActions,
  });

  const handleUpdatePermissions = () => {
    if (settingsState) {
      updatePermissions(settingsState);
    }
  };

  return (
    <Dialog open={true} onClose={onClose} fullWidth>
      <DialogContent sx={{ minHeight: 400 }}>
        <PermissionsSettings
          title={t('permission_dialog_title', { name: user.name })}
          permissions={permissions}
          onChange={setSettingsState}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>
          <T keyName="permission_dialog_close" />
        </Button>
        <LoadingButton
          loading={isLoading}
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
