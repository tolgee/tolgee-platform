import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  styled,
  Tooltip,
  Typography,
} from '@mui/material';
import { Trash01 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { useIsAdmin } from 'tg.globalContext/helpers';
import { AppSummary } from 'tg.component/apps/AppSummary';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

import { AppOrganizationSelect } from './AppOrganizationSelect';

type AppInstallModel = components['schemas']['AppInstallModel'];

const StyledList = styled('div')`
  display: grid;
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
`;

const StyledRow = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(2)};
  padding: ${({ theme }) => theme.spacing(1, 1, 1, 2)};

  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider};
  }
`;

const StyledEmpty = styled('div')`
  padding: ${({ theme }) => theme.spacing(2)};
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  install: AppInstallModel;
  onClose: () => void;
};

export const AppOrganizationsDialog = ({ install, onClose }: Props) => {
  const isAdmin = useIsAdmin();

  const organizationsLoadable = useApiQuery({
    url: '/v2/administration/apps/{installId}/organizations',
    method: 'get',
    path: { installId: install.id },
  });

  const grantMutation = useApiMutation({
    url: '/v2/administration/apps/{installId}/organizations/{organizationId}',
    method: 'put',
    invalidatePrefix: '/v2/administration/apps',
  });

  const revokeMutation = useApiMutation({
    url: '/v2/administration/apps/{installId}/organizations/{organizationId}',
    method: 'delete',
    invalidatePrefix: '/v2/administration/apps',
  });

  const grantAllMutation = useApiMutation({
    url: '/v2/administration/apps/{installId}/organizations/all',
    method: 'put',
    invalidatePrefix: '/v2/administration/apps',
  });

  const revokeAllMutation = useApiMutation({
    url: '/v2/administration/apps/{installId}/organizations/all',
    method: 'delete',
    invalidatePrefix: '/v2/administration/apps',
  });

  const organizations = organizationsLoadable.data?._embedded?.organizations;
  const items = organizations ?? [];
  const availableToAll = install.availableToAllOrganizations;
  const updating =
    grantMutation.isLoading ||
    revokeMutation.isLoading ||
    grantAllMutation.isLoading ||
    revokeAllMutation.isLoading;

  const handleAdd = (organizationId: number) => {
    grantMutation.mutate({
      path: { installId: install.id, organizationId },
    });
  };

  const handleAddAll = () => {
    grantAllMutation.mutate({
      path: { installId: install.id },
    });
  };

  const handleRemoveAll = () => {
    confirmation({
      title: (
        <T
          keyName="administration_apps_revoke_all_confirm_title"
          defaultValue="Stop making the app available to all organizations?"
        />
      ),
      message: (
        <T
          keyName="administration_apps_revoke_all_confirm_message"
          defaultValue="{appName} will only be available to the organizations listed below. Projects that had access only through this setting will lose it, while explicitly granted organizations keep it."
          params={{ appName: install.name }}
        />
      ),
      confirmButtonText: (
        <T
          keyName="administration_apps_revoke_all_confirm_button"
          defaultValue="Turn off"
        />
      ),
      onConfirm: () => {
        revokeAllMutation.mutate({
          path: { installId: install.id },
        });
      },
    });
  };

  const handleRemove = (organizationId: number, organizationName: string) => {
    confirmation({
      title: (
        <T
          keyName="administration_apps_revoke_confirm_title"
          defaultValue="Remove organization access?"
        />
      ),
      message: availableToAll ? (
        <T
          keyName="administration_apps_revoke_confirm_message_all"
          defaultValue="{organizationName} loses its explicit grant. {appName} stays available to it while the app is available to all organizations, but access ends as soon as that is turned off."
          params={{ appName: install.name, organizationName }}
        />
      ) : (
        <T
          keyName="administration_apps_revoke_confirm_message"
          defaultValue="{appName} will no longer be available to {organizationName}, and every project in that organization will lose access to it."
          params={{ appName: install.name, organizationName }}
        />
      ),
      onConfirm: () => {
        revokeMutation.mutate({
          path: { installId: install.id, organizationId },
        });
      },
    });
  };

  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      data-cy="administration-apps-organizations-dialog"
    >
      <DialogTitle>
        <T
          keyName="administration_apps_organizations_dialog_title"
          defaultValue="App availability"
        />
      </DialogTitle>

      <DialogContent>
        <Box mb={2}>
          <AppSummary
            name={install.name}
            version={install.version}
            url={install.baseUrl}
          />
        </Box>

        {availableToAll && (
          <Alert
            severity="info"
            data-cy="administration-apps-organizations-all-alert"
            sx={{ mb: 2 }}
            action={
              isAdmin && (
                <Button
                  data-cy="administration-apps-organizations-all-disable"
                  color="inherit"
                  size="small"
                  disabled={updating}
                  onClick={handleRemoveAll}
                >
                  <T
                    keyName="administration_apps_organizations_all_disable"
                    defaultValue="Turn off"
                  />
                </Button>
              )
            }
          >
            <T
              keyName="administration_apps_organizations_all_active"
              defaultValue="This app is available to all organizations, including organizations created later."
            />
          </Alert>
        )}

        <Typography variant="body2" color="text.secondary" mb={1}>
          {availableToAll ? (
            <T
              keyName="administration_apps_organizations_dialog_description_explicit"
              defaultValue="Organizations with an explicit grant. Only these keep the app if you turn off availability to all organizations."
            />
          ) : (
            <T
              keyName="administration_apps_organizations_dialog_description"
              defaultValue="Organizations allowed to enable this app in their projects."
            />
          )}
        </Typography>

        {organizationsLoadable.isLoading && (
          <Box display="flex" justifyContent="center" py={4}>
            <CircularProgress size={24} />
          </Box>
        )}

        {organizationsLoadable.error && (
          <Alert
            severity="error"
            data-cy="administration-apps-organizations-error"
          >
            {typeof organizationsLoadable.error.code === 'string' ? (
              <TranslatedError code={organizationsLoadable.error.code} />
            ) : (
              <T keyName="simple_paginated_list_error_message" />
            )}
          </Alert>
        )}

        {organizations && (
          <StyledList>
            {items.length === 0 && (
              <StyledEmpty data-cy="administration-apps-organizations-empty">
                {availableToAll ? (
                  <T
                    keyName="administration_apps_organizations_empty_explicit"
                    defaultValue="No organization has an explicit grant."
                  />
                ) : (
                  <T
                    keyName="administration_apps_organizations_empty"
                    defaultValue="This app is not available to any organization yet."
                  />
                )}
              </StyledEmpty>
            )}
            {items.map((organization) => (
              <StyledRow
                key={organization.id}
                data-cy="administration-apps-organizations-item"
                data-cy-organization-id={organization.id}
              >
                <Typography variant="body2">
                  {organization.name}{' '}
                  <Chip size="small" label={organization.id} />
                </Typography>
                {isAdmin && (
                  <Tooltip
                    title={
                      <T
                        keyName="administration_apps_organizations_remove_tooltip"
                        defaultValue="Remove organization"
                      />
                    }
                  >
                    <span>
                      <IconButton
                        data-cy="administration-apps-organizations-item-remove"
                        disabled={updating}
                        onClick={() =>
                          handleRemove(organization.id, organization.name)
                        }
                      >
                        <Trash01 />
                      </IconButton>
                    </span>
                  </Tooltip>
                )}
              </StyledRow>
            ))}
          </StyledList>
        )}

        {isAdmin && (
          <Box mt={2}>
            <AppOrganizationSelect
              excludedIds={items.map((organization) => organization.id)}
              disabled={updating}
              allOptionVisible={!availableToAll}
              onSelect={(organization) => handleAdd(organization.id)}
              onSelectAll={handleAddAll}
            />
          </Box>
        )}
      </DialogContent>

      <DialogActions>
        <Button
          data-cy="administration-apps-organizations-close"
          onClick={onClose}
        >
          <T keyName="global_close_button" defaultValue="Close" />
        </Button>
      </DialogActions>
    </Dialog>
  );
};
