import { PermissionSettingsState } from 'tg.component/PermissionsSettings/types';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { getInvitationPermissionBody } from './getInvitationPermissionBody';

type Props = {
  projectId: number;
};

export type CreateContributorInvitationData = {
  userId: number;
  permissions: PermissionSettingsState;
};

export const useCreateContributorInvitation = ({ projectId }: Props) => {
  const invite = useApiMutation({
    url: '/v2/projects/{projectId}/invite-contributor',
    method: 'put',
    invalidatePrefix: [
      '/v2/projects/{projectId}/invitations',
      '/v2/projects/{projectId}/contributors',
    ],
  });

  return {
    async createContributorInvitation({
      userId,
      permissions,
    }: CreateContributorInvitationData) {
      const result = getInvitationPermissionBody(permissions);
      if (!result.ok) {
        throw new Error('Incorrect data');
      }

      return invite.mutateAsync({
        path: { projectId },
        content: {
          'application/json': {
            userId,
            ...result.body,
          },
        },
      });
    },
    isLoading: invite.isLoading,
  };
};
