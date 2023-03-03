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

    const afterActions = {
      onSuccess() {
        messages.success(<T>permissions_set_message</T>);
        onClose();
      },
      onError(e) {
        parseErrorResponse(e).forEach((err) => messages.error(<T>{err}</T>));
      },
    };

    if (
      settingsState.tab === 'advanced' &&
      settingsState.advancedState.scopes
    ) {
      const languagePermissions: LanguagePermissions = {};

      settingsState.advancedState.scopes
        .map(getScopeLanguagePermission)
        .forEach((property) => {
          if (property) {
            languagePermissions[property] =
              settingsState.advancedState[property];
          }
        });

      editScopes.mutate(
        {
          path: { userId: user.id, projectId: project.id },
          query: {
            scopes: settingsState.advancedState.scopes,
            ...languagePermissions,
          },
        },
        afterActions
      );
    } else if (settingsState.tab === 'basic' && settingsState.basicState.role) {
      let languagePermissions: LanguagePermissions = {};
      const role = settingsState.basicState.role;

      if (role === 'REVIEW') {
        languagePermissions = {
          translateLanguages: settingsState.basicState.languages,
          stateChangeLanguages: settingsState.basicState.languages,
        };
      } else if (role === 'TRANSLATE') {
        languagePermissions = {
          translateLanguages: settingsState.basicState.languages,
        };
      }

      editRole.mutate(
        {
          path: {
            userId: user.id,
            projectId: project.id,
            permissionType: settingsState.basicState.role,
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
