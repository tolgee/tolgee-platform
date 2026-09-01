import { Box, IconButton, Tooltip, Typography, styled } from '@mui/material';
import { Settings01 } from '@untitled-ui/icons-react';
import { Link } from 'react-router-dom';
import { T } from '@tolgee/react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from '../../useOrganization';

import { OwnedAppHealthChip } from './OwnedAppHealthChip';

type Props = {
  organizationId: number;
};

const StyledContainer = styled('div')`
  display: grid;
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
  background: ${({ theme }) => theme.palette.background.paper};
`;

const StyledHeader = styled('div')`
  padding: ${({ theme }) => theme.spacing(2.5)};
`;

const StyledItem = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  padding: ${({ theme }) => theme.spacing(1.5, 2.5)};
  gap: ${({ theme }) => theme.spacing(2)};
`;

const StyledItemMeta = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
  min-width: 0;
`;

export const OwnedAppsSection = ({ organizationId }: Props) => {
  const organization = useOrganization();

  const appsLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/owned-apps',
    method: 'get',
    path: { organizationId },
  });

  const items = appsLoadable.data?._embedded?.ownedApps ?? [];

  return (
    <StyledContainer data-cy="organization-owned-apps-section">
      <StyledHeader>
        <Typography variant="h6">
          <T
            keyName="owned_apps_section_title"
            defaultValue="Apps you registered"
          />
        </Typography>
        <Typography variant="body2" color="text.secondary">
          <T
            keyName="owned_apps_section_description"
            defaultValue="Apps this organization registered on this server. You hold their app-level credentials and can take them off every organization that installed them."
          />
        </Typography>
      </StyledHeader>

      {!appsLoadable.isLoading && items.length === 0 && (
        <StyledItem>
          <Typography
            variant="body2"
            color="text.secondary"
            data-cy="organization-owned-apps-empty"
          >
            <T
              keyName="owned_apps_empty"
              defaultValue="This organization has not registered any app yet. Registering happens the first time anyone installs an app from its manifest URL."
            />
          </Typography>
        </StyledItem>
      )}

      {items.map((app) => (
        <StyledItem
          key={app.id}
          data-cy="organization-owned-apps-item"
          data-cy-app-id={app.appId}
        >
          <StyledItemMeta>
            <Box display="flex" alignItems="center" gap={1}>
              <Typography variant="subtitle1">{app.name}</Typography>
              <OwnedAppHealthChip app={app} />
            </Box>
            <Typography variant="body2" color="text.secondary" noWrap>
              {app.manifestUrl}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              <T
                keyName="owned_apps_install_count"
                defaultValue="Installed by {count, plural, one {# organization} other {# organizations}}"
                params={{ count: app.installCount }}
              />
            </Typography>
          </StyledItemMeta>

          <Tooltip
            title={
              <T
                keyName="owned_apps_settings_tooltip"
                defaultValue="App settings"
              />
            }
          >
            <IconButton
              data-cy="organization-owned-apps-item-settings"
              component={Link}
              to={
                organization
                  ? LINKS.ORGANIZATION_APP.build({
                      [PARAMS.ORGANIZATION_SLUG]: organization.slug,
                      [PARAMS.APP_ID]: app.id,
                    })
                  : ''
              }
            >
              <Settings01 />
            </IconButton>
          </Tooltip>
        </StyledItem>
      ))}
    </StyledContainer>
  );
};
