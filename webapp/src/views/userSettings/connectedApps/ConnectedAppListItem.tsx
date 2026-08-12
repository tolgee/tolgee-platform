import { components } from 'tg.service/apiSchema.generated';
import { Box, Button, Chip, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';
import { useScopeTranslations } from 'tg.component/PermissionsSettings/useScopeTranslations';
import { PermissionModelScope } from 'tg.component/PermissionsSettings/types';

const StyledRoot = styled(Box)`
  display: grid;
  gap: 0.5rem;

  @container (min-width: 899px) {
    grid-template-columns: 2fr 3fr auto auto;
  }

  border-bottom: 1px solid ${({ theme }) => theme.palette.emphasis.A100};
  align-items: center;

  &:last-child {
    border-bottom: none;
  }

  padding: 0.5rem;
`;

const StyledName = styled(Box)`
  font-weight: bold;
`;

const StyledScopes = styled(Box)`
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
`;

export function ConnectedAppListItem(props: {
  app: components['schemas']['ConnectedAppModel'];
}) {
  const message = useMessage();
  const { getScopeTranslation } = useScopeTranslations();

  const revokeMutation = useApiMutation({
    url: '/v2/user/connected-apps/{clientId}',
    method: 'delete',
    invalidatePrefix: '/v2/user/connected-apps',
    options: {
      onSuccess: () =>
        message.success(
          <T
            keyName="connected_apps_disconnected_message"
            defaultValue="App disconnected"
          />
        ),
    },
  });

  const onDisconnect = () => {
    confirmation({
      confirmButtonText: (
        <T
          keyName="connected_apps_disconnect_button"
          defaultValue="Disconnect"
        />
      ),
      message: (
        <Box>
          <Box sx={{ mb: 2 }}>
            <T
              keyName="connected_apps_disconnect_confirmation_message"
              defaultValue="This app will lose access to your account. Any of its active sessions will stop working. You can reconnect it later."
            />
          </Box>
          <b>{props.app.clientName}</b>
        </Box>
      ),
      onConfirm: () =>
        revokeMutation.mutate({ path: { clientId: props.app.clientId } }),
    });
  };

  return (
    <StyledRoot data-cy="connected-apps-list-item">
      <StyledName data-cy="connected-apps-list-item-name">
        {props.app.clientName}
      </StyledName>
      <StyledScopes>
        {props.app.scopes.map((scope) => (
          <Chip
            key={scope}
            size="small"
            label={getScopeTranslation(scope as PermissionModelScope)}
          />
        ))}
      </StyledScopes>
      <Box data-cy="connected-apps-list-item-last-authorized">
        {props.app.lastAuthorizedAt ? (
          <T
            keyName="connected_apps_last_authorized"
            defaultValue="Authorized {date, date}"
            params={{ date: props.app.lastAuthorizedAt }}
          />
        ) : null}
      </Box>
      <Box>
        <Button
          data-cy="connected-apps-list-item-disconnect-button"
          size="small"
          variant="outlined"
          color="error"
          onClick={onDisconnect}
        >
          <T keyName="connected_apps_disconnect" defaultValue="Disconnect" />
        </Button>
      </Box>
    </StyledRoot>
  );
}
