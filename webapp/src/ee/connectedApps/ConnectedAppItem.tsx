import { Box, Button, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';
import { SessionDate } from 'tg.ee.module/sessions/SessionDate';
import {
  CONNECTED_APPS_GRID_COLUMNS,
  CONNECTED_APPS_ROW_PADDING,
  CONNECTED_APPS_WIDE_LAYOUT,
} from './connectedAppsGrid';

const StyledRoot = styled(Box)`
  display: grid;
  gap: 4px 8px;
  align-items: center;
  padding: ${CONNECTED_APPS_ROW_PADDING};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  grid-template-areas:
    'app  action'
    'meta meta';
  grid-template-columns: 1fr auto;

  &:last-of-type {
    border-bottom: none;
  }

  @container (min-width: ${CONNECTED_APPS_WIDE_LAYOUT}) {
    grid-template-areas: 'app access projects authorized lastActive action';
    grid-template-columns: ${CONNECTED_APPS_GRID_COLUMNS};
  }
`;

/**
 * Narrow: one wrapping line of labelled facts. Wide: `display: contents` dissolves this wrapper so
 * the four cells become columns of the row's own grid.
 */
const StyledMeta = styled(Box)`
  grid-area: meta;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 16px;

  @container (min-width: ${CONNECTED_APPS_WIDE_LAYOUT}) {
    display: contents;
  }
`;

const StyledApp = styled(Box)`
  grid-area: app;
  min-width: 0;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: ${({ theme }) => theme.palette.text.primary};
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

  @container (min-width: ${CONNECTED_APPS_WIDE_LAYOUT}) {
    display: none;
  }
`;

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
  app: components['schemas']['ConnectedAppModel'];
};

export function ConnectedAppItem({ app }: Props) {
  const message = useMessage();

  const revokeMutation = useApiMutation({
    url: '/v2/user/connected-apps/{id}',
    method: 'delete',
    invalidatePrefix: '/v2/user/connected-apps',
    options: {
      onSuccess: () => {
        message.success(
          <T
            keyName="connected-app-revoked-message"
            defaultValue="App disconnected"
          />
        );
      },
    },
  });

  const onRevoke = () => {
    confirmation({
      confirmButtonText: (
        <T keyName="connected-app-revoke-button" defaultValue="Disconnect" />
      ),
      message: (
        <T
          keyName="connected-app-revoke-confirmation-message"
          defaultValue="Do you really want to disconnect this app? It will lose access to your account until you authorize it again."
        />
      ),
      onConfirm: () => revokeMutation.mutate({ path: { id: app.id } }),
    });
  };

  return (
    <StyledRoot
      data-cy="connected-app-list-item"
      data-cy-client-id={app.clientId}
    >
      <StyledApp>{app.clientName}</StyledApp>

      <StyledMeta>
        <StyledCell sx={{ gridArea: 'access' }}>
          <StyledInlineLabel>
            <T keyName="connected-apps-column-access" defaultValue="Access" />
          </StyledInlineLabel>
          {app.scopes.join(', ')}
        </StyledCell>

        <StyledCell
          sx={{ gridArea: 'projects' }}
          data-cy="connected-app-projects"
        >
          <StyledInlineLabel>
            <T
              keyName="connected-apps-column-projects"
              defaultValue="Projects"
            />
          </StyledInlineLabel>
          {app.allProjects ? (
            <T
              keyName="connected-app-all-projects"
              defaultValue="All projects"
            />
          ) : (
            app.projects.map((project) => project.name).join(', ')
          )}
        </StyledCell>

        <StyledCell sx={{ gridArea: 'authorized' }}>
          <StyledInlineLabel>
            <T
              keyName="connected-apps-column-authorized"
              defaultValue="Authorized"
            />
          </StyledInlineLabel>
          <SessionDate date={app.authorizedAt} />
        </StyledCell>

        <StyledCell sx={{ gridArea: 'lastActive' }}>
          <StyledInlineLabel>
            <T
              keyName="connected-apps-column-last-active"
              defaultValue="Last active"
            />
          </StyledInlineLabel>
          {app.lastActiveAt ? <SessionDate date={app.lastActiveAt} /> : '-'}
        </StyledCell>
      </StyledMeta>

      <StyledAction>
        <StyledRevokeButton
          data-cy="connected-app-revoke-button"
          size="small"
          disabled={revokeMutation.isLoading}
          onClick={onRevoke}
        >
          <T keyName="connected-app-revoke-button" defaultValue="Disconnect" />
        </StyledRevokeButton>
      </StyledAction>
    </StyledRoot>
  );
}
