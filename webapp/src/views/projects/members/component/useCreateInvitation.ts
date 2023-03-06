import {
  LanguageModel,
  LanguagePermissions,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import { getScopeLanguagePermission } from 'tg.ee/PermissionsAdvanced/hierarchyTools';
import { useApiMutation } from 'tg.service/http/useQueryApi';

type Props = {
  projectId: number;
  allLangs: LanguageModel[];
};

export type CreateInvitationData = {
  permissions: PermissionSettingsState;
  email?: string;
  name?: string;
};

export const useCreateInvitation = ({ projectId, allLangs }: Props) => {
  const invite = useApiMutation({
    url: '/v2/projects/{projectId}/invite',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/invitations',
  });

  return {
    async createInvitation({ permissions, email, name }: CreateInvitationData) {
      if (permissions.tab === 'advanced' && permissions.advancedState.scopes) {
        const languagePermissions: LanguagePermissions = {};

        permissions.advancedState.scopes
          .map(getScopeLanguagePermission)
          .forEach((property) => {
            if (property) {
              languagePermissions[property] =
                permissions.advancedState[property];
            }
          });

        return invite.mutateAsync({
          path: { projectId },
          content: {
            'application/json': {
              email,
              name,
              scopes: permissions.advancedState.scopes,
              ...languagePermissions,
            },
          },
        });
      } else if (permissions.tab === 'basic' && permissions.basicState.role) {
        let languagePermissions: LanguagePermissions = {};
        const role = permissions.basicState.role;

        if (role === 'REVIEW') {
          languagePermissions = {
            viewLanguages: allLangs.map((l) => l.id),
            translateLanguages: permissions.basicState.languages,
            stateChangeLanguages: permissions.basicState.languages,
          };
        } else if (role === 'TRANSLATE') {
          languagePermissions = {
            viewLanguages: allLangs.map((l) => l.id),
            translateLanguages: permissions.basicState.languages,
          };
        }

        return invite.mutateAsync({
          path: {
            projectId,
          },
          content: {
            'application/json': {
              type: role,
              email,
              name,
              ...languagePermissions,
            },
          },
        });
      }
      throw new Error('Incorrect data');
    },
    isLoading: invite.isLoading,
  };
};
