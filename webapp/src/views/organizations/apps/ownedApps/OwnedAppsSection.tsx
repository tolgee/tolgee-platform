import { useState } from 'react';
import {
  Box,
  Button,
  Link as MuiLink,
  Typography,
  styled,
} from '@mui/material';
import { Link } from 'react-router-dom';
import { T } from '@tolgee/react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AppAvatar } from 'tg.component/apps/AppAvatar';
import { AppsAlphaChip } from 'tg.component/apps/AppsAlphaBadge';
import { AppInfoHint } from 'tg.component/apps/AppInfoHint';
import { useOrganization } from '../../useOrganization';

import { OwnedAppHealthChip } from './OwnedAppHealthChip';
import { OwnedAppRegisterDialog } from './OwnedAppRegisterDialog';

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
  align-items: center;
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  padding: ${({ theme }) => theme.spacing(1.75, 2.5)};
  gap: ${({ theme }) => theme.spacing(1.75)};
`;

const StyledItemMeta = styled('div')`
  flex: 1;
  min-width: 0;
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.25)};
`;

const StyledMetaLine = styled('div')`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing(0.75)};
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;

const StyledAttentionLine = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.75)};
  margin-top: ${({ theme }) => theme.spacing(0.75)};
  color: ${({ theme }) => theme.palette.error.main};
  font-size: 13px;
  font-weight: 500;
`;

const StyledDot = styled('span')`
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex: none;
  background: ${({ theme }) => theme.palette.error.main};
`;

export const OwnedAppsSection = ({ organizationId }: Props) => {
  const organization = useOrganization();
  const [registerOpen, setRegisterOpen] = useState(false);

  const appsLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/owned-apps',
    method: 'get',
    path: { organizationId },
  });

  const items = appsLoadable.data?._embedded?.ownedApps ?? [];

  const detailLink = (appId: number) =>
    organization
      ? LINKS.ORGANIZATION_APP.build({
          [PARAMS.ORGANIZATION_SLUG]: organization.slug,
          [PARAMS.APP_ID]: appId,
        })
      : '';

  return (
    <StyledContainer data-cy="organization-owned-apps-section">
      <StyledHeader>
        <Box display="flex" alignItems="flex-start" gap={2}>
          <Box flex={1} minWidth={0}>
            <Box display="flex" alignItems="center" gap={1}>
              <Typography variant="h6">
                <T
                  keyName="owned_apps_section_title"
                  defaultValue="Apps you registered"
                />
              </Typography>
              <AppsAlphaChip />
            </Box>
            <Typography variant="body2" color="text.secondary">
              <T
                keyName="owned_apps_section_description"
                defaultValue="Apps registered by organization {name}. You hold their app-level credentials and can take them off every organization that installed them."
                params={{ name: organization?.name ?? '' }}
              />
            </Typography>
          </Box>
          <Button
            variant="contained"
            color="primary"
            data-cy="organization-owned-apps-register"
            onClick={() => setRegisterOpen(true)}
          >
            <T
              keyName="owned_apps_register_button"
              defaultValue="Register app"
            />
          </Button>
        </Box>
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
          <AppAvatar icon={app.icon} name={app.name} appId={app.appId} />
          <StyledItemMeta>
            <Box display="flex" alignItems="center" gap={1} minWidth={0}>
              <Typography variant="subtitle1" noWrap>
                {app.name}
              </Typography>
              <OwnedAppHealthChip app={app} />
            </Box>
            <StyledMetaLine>
              <span>
                <T
                  keyName="owned_apps_install_count"
                  defaultValue="Installed by {count, plural, one {# organization} other {# organizations}}"
                  params={{ count: app.installCount }}
                />
              </span>
              <span aria-hidden="true">·</span>
              <AppInfoHint
                dataCy="organization-owned-apps-item-info"
                entries={[
                  {
                    label: (
                      <T
                        keyName="owned_app_field_app_id"
                        defaultValue="App id"
                      />
                    ),
                    value: app.appId,
                  },
                  {
                    label: (
                      <T
                        keyName="owned_app_field_base_url"
                        defaultValue="Base URL"
                      />
                    ),
                    value: app.baseUrl,
                  },
                  {
                    label: (
                      <T
                        keyName="owned_app_field_manifest_url"
                        defaultValue="Manifest URL"
                      />
                    ),
                    value: app.manifestUrl,
                  },
                ]}
              />
            </StyledMetaLine>
            {app.unhealthySince != null && (
              <StyledAttentionLine data-cy="organization-owned-apps-item-unhealthy">
                <StyledDot />
                <span>
                  <T
                    keyName="owned_apps_unhealthy_inline"
                    defaultValue="Manifest checks are failing"
                  />
                </span>
                <MuiLink
                  component={Link}
                  color="inherit"
                  underline="always"
                  fontWeight={600}
                  sx={{
                    '&&': {
                      textDecoration: 'underline',
                      textUnderlineOffset: 3,
                    },
                  }}
                  to={detailLink(app.id)}
                  data-cy="organization-owned-apps-item-unhealthy-details"
                >
                  <T
                    keyName="owned_apps_unhealthy_details_link"
                    defaultValue="Details"
                  />
                </MuiLink>
              </StyledAttentionLine>
            )}
          </StyledItemMeta>

          <Button
            size="small"
            variant="outlined"
            component={Link}
            data-cy="organization-owned-apps-item-settings"
            to={detailLink(app.id)}
          >
            <T keyName="owned_apps_manage_button" defaultValue="Manage" />
          </Button>
        </StyledItem>
      ))}

      {registerOpen && (
        <OwnedAppRegisterDialog
          open={registerOpen}
          onClose={() => setRegisterOpen(false)}
        />
      )}
    </StyledContainer>
  );
};
