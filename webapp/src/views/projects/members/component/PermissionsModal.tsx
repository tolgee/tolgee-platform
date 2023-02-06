import { useState } from 'react';
import { T } from '@tolgee/react';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';

import {
  LanguagePermissions,
  PermissionModel,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { PermissionsSettings } from 'tg.component/PermissionsSettings/PermissionsSettings';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { getScopeLanguagePermission } from 'tg.ee/PermissionsAdvanced/hierarchyTools';

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

  const editRole = useApiMutation({
    url: '/v2/projects/{projectId}/users/{userId}/set-permissions/{permissionType}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/users',
  });

  const editScopes = useApiMutation({
    url: '/v2/projects/{projectId}/users/{userId}/set-permissions',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/users',
  });

  const [settingsState, setSettingsState] = useState<
    PermissionSettingsState | undefined
  >(undefined);

  const updatePermissions = () => {
    if (!settingsState) {
      return;
    }

    const languagePermissions: LanguagePermissions = {};

    settingsState.state.scopes
      .map(getScopeLanguagePermission)
      .forEach((property) => {
        if (property) {
          languagePermissions[property] = settingsState.state[property];
        }
      });

    const afterActions = {
      onSuccess() {
        messages.success(<T>permissions_set_message</T>);
        onClose();
      },
      onError(e) {
        parseErrorResponse(e).forEach((err) => messages.error(<T>{err}</T>));
      },
    };

    if (settingsState.tab === 'advanced' && settingsState.state.scopes) {
      editScopes.mutate(
        {
          path: { userId: user.id, projectId: project.id },
          query: {
            scopes: settingsState.state.scopes,
            ...languagePermissions,
          },
        },
        afterActions
      );
    } else if (settingsState.tab === 'basic' && settingsState.state.role) {
      editRole.mutate(
        {
          path: {
            userId: user.id,
            projectId: project.id,
            permissionType: settingsState.state.role,
          },
          query: {
            ...languagePermissions,
          },
        },
        afterActions
      );
    }
  };

  return (
    <Dialog open={true} onClose={onClose} fullWidth maxWidth="md">
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
          loading={editRole.isLoading}
          onClick={updatePermissions}
          color="primary"
          variant="contained"
        >
          <T keyName="permission_dialog_save" />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
