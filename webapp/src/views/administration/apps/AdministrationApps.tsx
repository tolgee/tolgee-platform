import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Button, Chip, ListItem, styled, Typography } from '@mui/material';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { LINKS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { AppSummary } from 'tg.component/apps/AppSummary';
import { AppChips } from 'tg.component/apps/AppChips';
import { confirmation } from 'tg.hooks/confirmation';
import { useIsAdmin } from 'tg.globalContext/helpers';

import { BaseAdministrationView } from '../components/BaseAdministrationView';
import { AppOrganizationsDialog } from './AppOrganizationsDialog';
import { RegisterNativeAppDialog } from './RegisterNativeAppDialog';

type AppInstallModel = components['schemas']['AppInstallModel'];

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  }
`;

const StyledMeta = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
  min-width: 0;
`;

export const AdministrationApps = () => {
  const { t } = useTranslate();
  const isAdmin = useIsAdmin();
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [registerOpen, setRegisterOpen] = useState(false);

  const appsLoadable = useApiQuery({
    url: '/v2/administration/apps',
    method: 'get',
    query: {
      page,
      size: 20,
      sort: ['name,asc'],
    },
    options: {
      keepPreviousData: true,
    },
  });

  const deregisterMutation = useApiMutation({
    url: '/v2/administration/apps/{installId}',
    method: 'delete',
    invalidatePrefix: '/v2/administration/apps',
  });

  const selected =
    appsLoadable.data?._embedded?.appInstalls?.find(
      (app) => app.id === selectedId
    ) ?? null;

  const handleDeregister = (app: AppInstallModel) => {
    confirmation({
      title: (
        <T
          keyName="administration_apps_deregister_confirm_title"
          defaultValue="Deregister app?"
        />
      ),
      message: (
        <T
          keyName="administration_apps_deregister_confirm_message"
          defaultValue="{appName} will be removed from all organizations and from every project it is enabled in. Its client credentials will stop working."
          params={{ appName: app.name }}
        />
      ),
      confirmButtonText: (
        <T
          keyName="administration_apps_deregister_confirm_button"
          defaultValue="Deregister"
        />
      ),
      onConfirm: () => {
        deregisterMutation.mutate({ path: { installId: app.id } });
      },
    });
  };

  return (
    <StyledWrapper>
      <DashboardPage>
        <BaseAdministrationView
          windowTitle={t('administration_apps', 'Apps')}
          navigation={[
            [
              t('administration_apps', 'Apps'),
              LINKS.ADMINISTRATION_APPS.build(),
            ],
          ]}
          allCentered
          hideChildrenOnLoading={false}
          loading={appsLoadable.isFetching}
        >
          <Box
            mb={2}
            display="flex"
            alignItems="flex-start"
            justifyContent="space-between"
            gap={2}
          >
            <Typography variant="body2" color="text.secondary">
              <T
                keyName="administration_apps_description"
                defaultValue="Apps registered directly against this server. Choose which organizations may enable them in their projects."
              />
            </Typography>
            {isAdmin && (
              <Button
                data-cy="administration-apps-register-button"
                variant="contained"
                color="primary"
                onClick={() => setRegisterOpen(true)}
              >
                <T
                  keyName="administration_apps_register_button"
                  defaultValue="Register app"
                />
              </Button>
            )}
          </Box>

          <PaginatedHateoasList
            wrapperComponentProps={{ className: 'listWrapper' }}
            onPageChange={setPage}
            loadable={appsLoadable}
            emptyPlaceholder={
              <EmptyListMessage>
                <T
                  keyName="administration_apps_empty"
                  defaultValue="No server-level apps are registered."
                />
              </EmptyListMessage>
            }
            renderItem={(app) => (
              <ListItem
                data-cy="administration-apps-list-item"
                data-cy-app-id={app.appId}
                sx={{
                  display: 'grid',
                  gridTemplateColumns: '1fr auto',
                  gap: 2,
                }}
              >
                <StyledMeta>
                  <AppSummary
                    name={app.name}
                    version={app.version}
                    url={app.baseUrl}
                  />
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    data-cy="administration-apps-item-app-id"
                  >
                    {app.appId}
                  </Typography>
                  <AppChips
                    items={app.scopes}
                    variant="outlined"
                    dataCy="administration-apps-item-scopes"
                    label={
                      <T
                        keyName="app_scopes_chips_label"
                        defaultValue="Permissions"
                      />
                    }
                    tooltip={
                      <T
                        keyName="app_scopes_chips_tooltip"
                        defaultValue="Permissions the app was granted when it was registered. They apply in every project the app is enabled for."
                      />
                    }
                    emptyLabel={
                      <T
                        keyName="app_scopes_chips_empty"
                        defaultValue="No permissions granted"
                      />
                    }
                  />
                  {app.availableToAllOrganizations && (
                    <Box>
                      <Chip
                        size="small"
                        color="info"
                        data-cy="administration-apps-item-all-organizations"
                        label={
                          <T
                            keyName="administration_apps_item_all_organizations"
                            defaultValue="All organizations"
                          />
                        }
                      />
                    </Box>
                  )}
                </StyledMeta>
                <Box display="flex" alignItems="center" gap={1}>
                  <Button
                    data-cy="administration-apps-item-organizations-button"
                    variant="contained"
                    onClick={() => setSelectedId(app.id)}
                  >
                    <T
                      keyName="administration_apps_manage_organizations"
                      defaultValue="Organizations"
                    />
                  </Button>
                  {isAdmin && (
                    <Button
                      data-cy="administration-apps-item-deregister-button"
                      variant="outlined"
                      color="error"
                      disabled={deregisterMutation.isLoading}
                      onClick={() => handleDeregister(app)}
                    >
                      <T
                        keyName="administration_apps_deregister"
                        defaultValue="Deregister"
                      />
                    </Button>
                  )}
                </Box>
              </ListItem>
            )}
          />
        </BaseAdministrationView>
      </DashboardPage>

      <RegisterNativeAppDialog
        open={registerOpen}
        onClose={() => setRegisterOpen(false)}
      />

      {selected && (
        <AppOrganizationsDialog
          install={selected}
          onClose={() => setSelectedId(null)}
        />
      )}
    </StyledWrapper>
  );
};
