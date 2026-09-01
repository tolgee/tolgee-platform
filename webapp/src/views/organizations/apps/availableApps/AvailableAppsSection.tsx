import { useState } from 'react';
import { Box, Button, Typography, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';

import { RegisterAppDialog } from 'tg.views/organizations/apps/registeredApps/RegisterAppDialog';

type AvailableAppModel = components['schemas']['AvailableAppModel'];

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

export const AvailableAppsSection = ({ organizationId }: Props) => {
  const [installing, setInstalling] = useState<AvailableAppModel | null>(null);

  const availableLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/apps/available',
    method: 'get',
    path: { organizationId },
  });

  const items = availableLoadable.data?._embedded?.availableApps ?? [];

  if (!availableLoadable.isLoading && items.length === 0) {
    return null;
  }

  return (
    <>
      <StyledContainer data-cy="organization-available-apps-section">
        <StyledHeader>
          <Typography variant="h6">
            <T
              keyName="available_apps_section_title"
              defaultValue="Available on this server"
            />
          </Typography>
          <Typography variant="body2" color="text.secondary">
            <T
              keyName="available_apps_section_description"
              defaultValue="Apps a server admin has offered to every organization. Install one to enable it in your projects."
            />
          </Typography>
        </StyledHeader>

        {items.map((app) => (
          <StyledItem
            key={app.id}
            data-cy="organization-available-apps-item"
            data-cy-app-id={app.appId}
          >
            <StyledItemMeta>
              <Typography variant="subtitle1">{app.name}</Typography>
              <Typography variant="body2" color="text.secondary" noWrap>
                {app.baseUrl}
              </Typography>
            </StyledItemMeta>
            <Box display="flex" gap={1}>
              <Button
                data-cy="organization-available-apps-item-install"
                variant="contained"
                color="primary"
                onClick={() => setInstalling(app)}
              >
                <T
                  keyName="available_apps_install_button"
                  defaultValue="Install"
                />
              </Button>
            </Box>
          </StyledItem>
        ))}
      </StyledContainer>

      {installing && (
        <RegisterAppDialog
          key={installing.id}
          open
          initialManifestUrl={installing.manifestUrl}
          onClose={() => setInstalling(null)}
        />
      )}
    </>
  );
};
