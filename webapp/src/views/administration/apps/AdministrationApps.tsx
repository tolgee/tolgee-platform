import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Button, ListItem, styled, Typography } from '@mui/material';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { LINKS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { AppSummary } from 'tg.component/apps/AppSummary';
import { AppChips } from 'tg.component/apps/AppChips';

import { BaseAdministrationView } from '../components/BaseAdministrationView';
import { AppOrganizationsDialog } from './AppOrganizationsDialog';

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
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<AppInstallModel | null>(null);

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
          <Box mb={2}>
            <Typography variant="body2" color="text.secondary">
              <T
                keyName="administration_apps_description"
                defaultValue="Apps registered directly against this server. Choose which organizations may enable them in their projects."
              />
            </Typography>
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
                  />
                </StyledMeta>
                <Box display="flex" alignItems="center">
                  <Button
                    data-cy="administration-apps-item-organizations-button"
                    variant="contained"
                    onClick={() => setSelected(app)}
                  >
                    <T
                      keyName="administration_apps_manage_organizations"
                      defaultValue="Organizations"
                    />
                  </Button>
                </Box>
              </ListItem>
            )}
          />
        </BaseAdministrationView>
      </DashboardPage>

      {selected && (
        <AppOrganizationsDialog
          install={selected}
          onClose={() => setSelected(null)}
        />
      )}
    </StyledWrapper>
  );
};
