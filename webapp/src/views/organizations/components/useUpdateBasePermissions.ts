import {
  LanguagePermissions,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { getScopeLanguagePermission } from 'tg.component/PermissionsSettings/hierarchyTools';

type Props = {
  organizationId?: number;
};

export const useUpdateBasePermissions = ({ organizationId }: Props) => {
  const editRole = useApiMutation({
    url: '/v2/organizations/{organizationId}/set-base-permissions/{permissionType}',
    method: 'put',
    invalidatePrefix: '/v2/organizations/{slug}',
  });

  const editScopes = useApiMutation({
    url: '/v2/organizations/{organizationId}/set-base-permissions',
    method: 'put',
    invalidatePrefix: '/v2/organizations/{slug}',
  });

  const updatePermissions = async (settingsState: PermissionSettingsState) => {
    if (!settingsState || !organizationId) {
      return;
    }

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

      await editScopes.mutateAsync({
        path: { organizationId },
        query: {
          scopes: settingsState.advancedState.scopes,
          ...languagePermissions,
        },
      });
    } else if (settingsState.tab === 'basic' && settingsState.basicState.role) {
      const role = settingsState.basicState.role;

      await editRole.mutateAsync({
        path: {
          organizationId,
          permissionType: role,
        },
      });
    }
  };

  return {
    isLoading: editRole.isLoading || editScopes.isLoading,
    updatePermissions,
  };
};
