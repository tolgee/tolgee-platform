import {
  LanguagePermissions,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import { getScopeLanguagePermission } from 'tg.ee/PermissionsAdvanced/hierarchyTools';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';

type ProjectInvitationModel = components['schemas']['ProjectInvitationModel'];

type Props = {
  projectId: number;
  after: {
    onSuccess(data: ProjectInvitationModel): void;
    onError(e: any): void;
  };
};

type MutateProps = {
  permissions: PermissionSettingsState;
  email?: string;
  name?: string;
};

export const useCreateInvitation = ({ after, projectId }: Props) => {
  const invite = useApiMutation({
    url: '/v2/projects/{projectId}/invite',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/invitations',
  });

  return {
    createInvitation: ({ permissions, email, name }: MutateProps) => {
      if (!permissions) {
        return;
      }

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

        invite.mutate(
          {
            path: { projectId },
            content: {
              'application/json': {
                email,
                name,
                scopes: permissions.advancedState.scopes,
                ...languagePermissions,
              },
            },
          },
          after
        );
      } else if (permissions.tab === 'basic' && permissions.basicState.role) {
        let languagePermissions: LanguagePermissions = {};
        const role = permissions.basicState.role;

        if (role === 'REVIEW') {
          languagePermissions = {
            translateLanguages: permissions.basicState.languages,
            stateChangeLanguages: permissions.basicState.languages,
          };
        } else if (role === 'TRANSLATE') {
          languagePermissions = {
            translateLanguages: permissions.basicState.languages,
          };
        }

        invite.mutate(
          {
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
          },
          after
        );
      }
    },
    isLoading: invite.isLoading,
  };
};
