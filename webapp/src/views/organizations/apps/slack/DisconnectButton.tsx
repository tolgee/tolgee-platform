import { LoadingButton } from '@mui/lab';
import { T } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';

type Props = {
  workspaceId: number;
};

export const DisconnectButton = ({ workspaceId }: Props) => {
  const organization = useOrganization()!;

  if (!organization) return null;

  const disconnectMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/slack/workspaces/{workspaceId}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations/{organizationId}/slack/workspaces',
  });

  const onDisconnect = () => {
    disconnectMutation.mutate({
      path: {
        organizationId: organization.id,
        workspaceId: workspaceId,
      },
    });
  };

  return (
    <LoadingButton
      loading={disconnectMutation.isLoading}
      size="medium"
      color="primary"
      onClick={onDisconnect}
    >
      <T keyName="slack_app_workspace_disconnect" />
    </LoadingButton>
  );
};
