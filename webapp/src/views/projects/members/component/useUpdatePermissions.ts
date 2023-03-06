import {
  LanguageModel,
  LanguagePermissions,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import { getScopeLanguagePermission } from 'tg.ee/PermissionsAdvanced/hierarchyTools';
import { useApiMutation } from 'tg.service/http/useQueryApi';

type Props = {
  userId: number;
  projectId: number;
  allLangs: LanguageModel[];
};

export const useUpdatePermissions = ({
  userId,
  projectId,
  allLangs,
}: Props) => {
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

  return {
    isLoading: editRole.isLoading || editScopes.isLoading,
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
        let languagePermissions: LanguagePermissions = {};
        const role = settingsState.basicState.role;

        if (role === 'REVIEW') {
          languagePermissions = {
            viewLanguages: allLangs.map((l) => l.id),
            translateLanguages: settingsState.basicState.languages,
            stateChangeLanguages: settingsState.basicState.languages,
          };
        } else if (role === 'TRANSLATE') {
          languagePermissions = {
            viewLanguages: allLangs.map((l) => l.id),
            translateLanguages: settingsState.basicState.languages,
          };
        }

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
