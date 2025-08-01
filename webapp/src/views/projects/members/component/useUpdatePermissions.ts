import {
  LanguagePermissions,
  PermissionModelRole,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { getScopeLanguagePermission } from 'tg.component/PermissionsSettings/hierarchyTools';

type Props = {
  userId: number;
  projectId: number;
};

export function languagePermissionsForRole(
  role: PermissionModelRole,
  languages: number[] | undefined
) {
  let languagePermissions: LanguagePermissions = {};
  if (role === 'REVIEW') {
    languagePermissions = {
      translateLanguages: languages,
      stateChangeLanguages: languages,
      suggestLanguages: languages,
    };
  } else if (role === 'TRANSLATE') {
    languagePermissions = {
      translateLanguages: languages,
      suggestLanguages: languages,
    };
  }
  return languagePermissions;
}

export const useUpdatePermissions = ({ userId, projectId }: Props) => {
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

  const setByOrganization = useApiMutation({
    url: '/v2/projects/{projectId}/users/{userId}/set-by-organization',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/users',
  });

  return {
    isLoading: editRole.isLoading || editScopes.isLoading,
    async setByOrganization() {
      await setByOrganization.mutateAsync({ path: { projectId, userId } });
    },
    async updatePermissions(settingsState: PermissionSettingsState) {
      if (!settingsState) {
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
          path: { userId, projectId },
          query: {
            scopes: settingsState.advancedState.scopes,
            ...languagePermissions,
          },
        });
      } else if (
        settingsState.tab === 'basic' &&
        settingsState.basicState.role
      ) {
        const role = settingsState.basicState.role;

        const languagePermissions = languagePermissionsForRole(
          role,
          settingsState.basicState.languages
        );

        await editRole.mutateAsync({
          path: {
            userId,
            projectId,
            permissionType: settingsState.basicState.role,
          },
          query: {
            ...languagePermissions,
          },
        });
      }
    },
  };
};
