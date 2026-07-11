import { T } from '@tolgee/react';

import {
  LanguageModel,
  LanguagePermissions,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { getScopeLanguagePermission } from 'tg.component/PermissionsSettings/hierarchyTools';
import { languagePermissionsForRole } from './useUpdatePermissions';

type Props = {
  projectId: number;
  allLangs: LanguageModel[];
};

export type CreateInvitationData = {
  permissions: PermissionSettingsState;
  email?: string;
  name?: string;
  agency?: string;
};

export const useCreateInvitation = ({ projectId, allLangs }: Props) => {
  const invite = useApiMutation({
    url: '/v2/projects/{projectId}/invite',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/invitations',
  });

  const messages = useMessage();

  return {
    async createInvitation({
      permissions,
      email,
      name,
      agency,
    }: CreateInvitationData) {
      if (permissions.tab === 'advanced' && permissions.advancedState.scopes) {
        if (permissions.advancedState.scopes.length === 0) {
          messages.error(<T keyName="scopes_at_least_one_scope_error" />);
          throw new Error('Incorrect data');
        }

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
              agencyId: Number(agency),
              scopes: permissions.advancedState.scopes,
              ...languagePermissions,
            },
          },
        });
      } else if (permissions.tab === 'basic' && permissions.basicState.role) {
        const role = permissions.basicState.role;

        const languagePermissions = languagePermissionsForRole(
          role,
          permissions.basicState.languages
        );

        return invite.mutateAsync({
          path: {
            projectId,
          },
          content: {
            'application/json': {
              type: role,
              email,
              name,
              agencyId: Number(agency),
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
