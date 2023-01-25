import { T } from '@tolgee/react';
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogActions,
  Button,
} from '@mui/material';

import {
  PermissionModel,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { PermissionsSettings } from 'tg.component/PermissionsSettings/PermissionsSettings';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useState } from 'react';

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
  const messages = useMessage();
  const project = useProject();

  const editPermission = useApiMutation({
    url: '/v2/projects/{projectId}/users/{userId}/set-permissions/{permissionType}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/users',
  });

  const [settingsState, setSettingsState] = useState<
    PermissionSettingsState | undefined
  >(undefined);

  const changePermission = () => {
    if (!settingsState?.basic?.role) {
      return;
    }
    const permissionType = settingsState.basic.role;
    editPermission.mutate(
      {
        path: {
          userId: user?.id,
          permissionType,
          projectId: project.id,
        },
        query: {},
      },
      {
        onSuccess() {
          messages.success(<T>permissions_set_message</T>);
          onClose();
        },
        onError(e) {
          parseErrorResponse(e).forEach((err) => messages.error(<T>{err}</T>));
        },
      }
    );
  };

  return (
    <Dialog open={true} onClose={onClose} fullWidth>
      <DialogTitle>
        <T keyName="permission_dialog_title" params={{ name: user.name }} />
      </DialogTitle>
      <DialogContent sx={{ minHeight: 400 }}>
        <PermissionsSettings
          permissions={permissions}
          onChange={setSettingsState}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>
          <T keyName="permission_dialog_close" />
        </Button>
        <LoadingButton
          loading={editPermission.isLoading}
          onClick={
            settingsState?.tab === 'basic' ? changePermission : undefined
          }
          color="primary"
          variant="contained"
        >
          <T keyName="permission_dialog_save" />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
