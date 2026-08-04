import { Box, Button, Chip, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

import { getUserAgentDisplay } from './getUserAgentDisplay';
import { SessionAuthTypeLabel } from './SessionAuthTypeLabel';
import { SessionLocation } from './SessionLocation';

const StyledRoot = styled(Box)`
  display: grid;
  gap: 0.5rem;

  @container (min-width: 899px) {
    grid-template-columns: 2fr 1fr 1fr 1fr auto;
  }

  border-bottom: 1px solid ${({ theme }) => theme.palette.emphasis.A100};
  align-items: center;

  &:last-child {
    border-bottom: none;
  }

  padding: 0.5rem;
`;

const StyledDevice = styled(Box)`
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: bold;
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledCaption = styled(Box)`
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  session: components['schemas']['UserSessionModel'];
};

export function SessionListItem({ session }: Props) {
  const message = useMessage();
  const { logout } = useGlobalActions();

  const revokeMutation = useApiMutation({
    url: '/v2/user/sessions/{id}',
    method: 'delete',
    invalidatePrefix: '/v2/user/sessions',
    options: {
      onSuccess: () => {
        if (session.isCurrent) {
          logout();
        } else {
          message.success(
            <T
              keyName="session-revoked-message"
              defaultValue="Session revoked"
            />
          );
        }
      },
    },
  });

  const onRevoke = () => {
    confirmation({
      confirmButtonText: (
        <T keyName="session-revoke-button" defaultValue="Revoke" />
      ),
      message: session.isCurrent ? (
        <T
          keyName="session-revoke-current-confirmation-message"
          defaultValue="This is the session you are using right now. Revoking it will log you out immediately. Do you want to continue?"
        />
      ) : (
        <T
          keyName="session-revoke-confirmation-message"
          defaultValue="Do you really want to revoke this session? The device using it will be logged out."
        />
      ),
      onConfirm: () => revokeMutation.mutate({ path: { id: session.id } }),
    });
  };

  const device = session.userAgent
    ? getUserAgentDisplay(session.userAgent)
    : undefined;

  return (
    <StyledRoot data-cy="session-list-item" data-cy-session-ip={session.ip}>
      <Box>
        <StyledDevice data-cy="session-list-item-device" title={device?.title}>
          {device ? (
            device.label
          ) : (
            <T
              keyName="session-item-unknown-device"
              defaultValue="Unknown device"
            />
          )}
          {session.isCurrent && (
            <Chip
              data-cy="session-list-item-current-badge"
              size="small"
              color="primary"
              label={
                <T keyName="session-current-badge" defaultValue="This device" />
              }
            />
          )}
        </StyledDevice>
        <StyledCaption>
          <SessionAuthTypeLabel type={session.type} />
        </StyledCaption>
      </Box>
      <SessionLocation session={session} />
      <Box data-cy="session-list-item-created">
        <T
          keyName="session-item-created"
          defaultValue="Signed in on {date, date, long}"
          params={{ date: session.createdAt }}
        />
      </Box>
      <Box data-cy="session-list-item-last-used">
        {session.lastUsedAt ? (
          <T
            keyName="session-item-last-used"
            defaultValue="Last used on {date, date, long}"
            params={{ date: session.lastUsedAt }}
          />
        ) : (
          <T keyName="session-item-never-used" defaultValue="Never used" />
        )}
      </Box>
      <Box>
        <Button
          data-cy="session-list-item-revoke-button"
          size="small"
          variant="outlined"
          color="error"
          onClick={onRevoke}
        >
          <T keyName="session-revoke-button" defaultValue="Revoke" />
        </Button>
      </Box>
    </StyledRoot>
  );
}
