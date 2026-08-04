import { Box, Button, Chip, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';
import { getUserAgentDisplay } from './getUserAgentDisplay';
import { SessionAuthTypeLabel } from './SessionAuthTypeLabel';
import { SessionLocation } from './SessionLocation';
import { SessionDate } from './SessionDate';
import {
  SESSIONS_GRID_COLUMNS,
  SESSIONS_ROW_PADDING,
  SESSIONS_WIDE_LAYOUT,
} from './sessionsGrid';

const StyledRoot = styled(Box)`
  display: grid;
  gap: 4px 8px;
  align-items: center;
  padding: ${SESSIONS_ROW_PADDING};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  grid-template-areas:
    'device action'
    'meta   meta';
  grid-template-columns: 1fr auto;

  &:last-of-type {
    border-bottom: none;
  }

  @container (min-width: ${SESSIONS_WIDE_LAYOUT}) {
    grid-template-areas: 'device location created lastUsed action';
    grid-template-columns: ${SESSIONS_GRID_COLUMNS};
    align-items: center;
  }
`;

/**
 * Narrow: one wrapping line of labelled facts. Wide: `display: contents` dissolves this wrapper so
 * the three cells become columns of the row's own grid.
 */
const StyledMeta = styled(Box)`
  grid-area: meta;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 16px;

  @container (min-width: ${SESSIONS_WIDE_LAYOUT}) {
    display: contents;
  }
`;

const StyledDevice = styled(Box)`
  grid-area: device;
  min-width: 0;
`;

const StyledDeviceName = styled(Box)`
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledDeviceLabel = styled('span')`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledCaption = styled(Box)`
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledCell = styled(Box)`
  display: flex;
  align-items: baseline;
  gap: 6px;
  min-width: 0;
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

/** The column headings only exist in the wide layout, so narrow rows label themselves. */
const StyledInlineLabel = styled('span')`
  flex-shrink: 0;
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.disabled};

  @container (min-width: ${SESSIONS_WIDE_LAYOUT}) {
    display: none;
  }
`;

/** One row per session means a wall of buttons - kept quiet until hovered. */
const StyledRevokeButton = styled(Button)`
  color: ${({ theme }) => theme.palette.text.secondary};

  &:hover {
    color: ${({ theme }) => theme.palette.error.main};
    background: ${({ theme }) => theme.palette.error.main}14;
  }
`;

const StyledAction = styled(Box)`
  grid-area: action;
  justify-self: end;
`;

type Props = {
  session: components['schemas']['UserSessionModel'];
};

export function SessionListItem({ session }: Props) {
  const message = useMessage();

  const revokeMutation = useApiMutation({
    url: '/v2/user/sessions/{id}',
    method: 'delete',
    invalidatePrefix: '/v2/user/sessions',
    options: {
      onSuccess: () => {
        message.success(
          <T keyName="session-revoked-message" defaultValue="Session revoked" />
        );
      },
    },
  });

  const onRevoke = () => {
    confirmation({
      confirmButtonText: (
        <T keyName="session-revoke-button" defaultValue="Revoke" />
      ),
      message: (
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
      <StyledDevice>
        <StyledDeviceName
          data-cy="session-list-item-device"
          title={device?.title}
        >
          <StyledDeviceLabel>
            {device ? (
              device.label
            ) : (
              <T
                keyName="session-item-unknown-device"
                defaultValue="Unknown device"
              />
            )}
          </StyledDeviceLabel>
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
        </StyledDeviceName>
        <StyledCaption>
          <SessionAuthTypeLabel type={session.type} />
        </StyledCaption>
      </StyledDevice>

      <StyledMeta>
        <StyledCell sx={{ gridArea: 'location' }}>
          <StyledInlineLabel>
            <T keyName="sessions-column-location" defaultValue="Location" />
          </StyledInlineLabel>
          <SessionLocation session={session} />
        </StyledCell>

        <StyledCell
          sx={{ gridArea: 'created' }}
          data-cy="session-list-item-created"
        >
          <StyledInlineLabel>
            <T keyName="sessions-column-signed-in" defaultValue="Signed in" />
          </StyledInlineLabel>
          <SessionDate date={session.createdAt} />
        </StyledCell>

        <StyledCell
          sx={{ gridArea: 'lastUsed' }}
          data-cy="session-list-item-last-used"
        >
          <StyledInlineLabel>
            <T keyName="sessions-column-last-used" defaultValue="Last used" />
          </StyledInlineLabel>
          {session.isCurrent ? (
            // The stamp is debounced, so for the session making this very request any timestamp
            // is stale on arrival.
            <T keyName="session-last-used-now" defaultValue="Active now" />
          ) : (
            // A session exists because someone signed in, so it was used at least once.
            <SessionDate date={session.lastUsedAt ?? session.createdAt} />
          )}
        </StyledCell>
      </StyledMeta>

      <StyledAction>
        {!session.isCurrent && (
          <StyledRevokeButton
            data-cy="session-list-item-revoke-button"
            size="small"
            variant="text"
            onClick={onRevoke}
          >
            <T keyName="session-revoke-button" defaultValue="Revoke" />
          </StyledRevokeButton>
        )}
      </StyledAction>
    </StyledRoot>
  );
}
