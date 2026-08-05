import { useState } from 'react';
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

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { useIsAdmin } from 'tg.globalContext/helpers';
import { AppSummary } from 'tg.component/apps/AppSummary';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

import {
  AppOrganizationSelect,
  SelectableOrganization,
} from './AppOrganizationSelect';
import {
  AppOrganizationProjectsSelect,
  SelectableProject,
} from './AppOrganizationProjectsSelect';

type AppInstallModel = components['schemas']['AppInstallModel'];

const PROJECTS_PAGE_SIZE = 100;

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

const StyledEnroll = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(1)};
  margin-bottom: ${({ theme }) => theme.spacing(3)};
`;

type EnrollResult = {
  organizationName: string;
  enabledCount: number;
  failedProjects: SelectableProject[];
};

type Props = {
  install: AppInstallModel;
  onClose: () => void;
};

export const AppOrganizationsDialog = ({ install, onClose }: Props) => {
  const isAdmin = useIsAdmin();

  const [organization, setOrganization] =
    useState<SelectableOrganization | null>(null);
  const [selectedProjectIds, setSelectedProjectIds] = useState<number[]>([]);
  const [result, setResult] = useState<EnrollResult | null>(null);

  const organizationsLoadable = useApiQuery({
    url: '/v2/administration/apps/{installId}/organizations',
    method: 'get',
    path: { installId: install.id },
  });

  const projectsLoadable = useApiQuery({
    url: '/v2/organizations/{id}/projects',
    method: 'get',
    path: { id: organization?.id ?? 0 },
    query: { size: PROJECTS_PAGE_SIZE, sort: ['name,asc'] },
    options: {
      enabled: Boolean(organization),
      noGlobalLoading: true,
    },
  });

  const grantMutation = useApiMutation({
    url: '/v2/administration/apps/{installId}/organizations/{organizationId}',
    method: 'put',
    invalidatePrefix: '/v2/administration/apps',
  });

  const enableMutation = useApiMutation({
    url: '/v2/projects/{projectId}/apps/{installId}',
    method: 'put',
    invalidatePrefix: '/v2/projects',
    // Failures are collected and reported per project below, not as a toast per call.
    options: { onError: () => undefined },
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
  const enrolling = grantMutation.isLoading || enableMutation.isLoading;
  const updating =
    enrolling ||
    revokeMutation.isLoading ||
    grantAllMutation.isLoading ||
    revokeAllMutation.isLoading;

  const projects: SelectableProject[] = (
    projectsLoadable.data?._embedded?.projects ?? []
  ).map((project) => ({ id: project.id, name: project.name }));
  const projectsTruncated =
    (projectsLoadable.data?.page?.totalElements ?? 0) > projects.length;

  const handleSelectOrganization = (
    selected: SelectableOrganization | null
  ) => {
    setOrganization(selected);
    setSelectedProjectIds([]);
    setResult(null);
  };

  const handleEnroll = async () => {
    if (!organization) return;
    setResult(null);

    try {
      await grantMutation.mutateAsync({
        path: { installId: install.id, organizationId: organization.id },
      });
    } catch (e) {
      return;
    }

    const failedProjects: SelectableProject[] = [];
    let enabledCount = 0;
    for (const projectId of selectedProjectIds) {
      try {
        await enableMutation.mutateAsync({
          path: { projectId, installId: install.id },
        });
        enabledCount += 1;
      } catch (e) {
        failedProjects.push(
          projects.find((project) => project.id === projectId) ?? {
            id: projectId,
            name: String(projectId),
          }
        );
      }
    }

    setResult({
      organizationName: organization.name,
      enabledCount,
      failedProjects,
    });
    setSelectedProjectIds(failedProjects.map((project) => project.id));
  };

  const handleAddAll = () => {
    handleSelectOrganization(null);
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

        {isAdmin && (
          <StyledEnroll>
            <Typography variant="subtitle2">
              <T
                keyName="administration_apps_organizations_enroll_title"
                defaultValue="Add an organization"
              />
            </Typography>
            <Typography variant="body2" color="text.secondary">
              <T
                keyName="administration_apps_organizations_enroll_description"
                defaultValue="Grant an organization access and switch the app on for its projects in one step. Projects where it is already enabled are left as they are."
              />
            </Typography>

            <AppOrganizationSelect
              value={organization}
              disabled={updating}
              allOptionVisible={!availableToAll}
              onChange={handleSelectOrganization}
              onSelectAll={handleAddAll}
            />

            {organization && projectsLoadable.error && (
              <Alert
                severity="error"
                data-cy="administration-apps-projects-error"
              >
                {typeof projectsLoadable.error.code === 'string' ? (
                  <TranslatedError code={projectsLoadable.error.code} />
                ) : (
                  <T keyName="simple_paginated_list_error_message" />
                )}
              </Alert>
            )}

            {organization && !projectsLoadable.error && (
              <AppOrganizationProjectsSelect
                projects={projects}
                loading={projectsLoadable.isLoading}
                truncated={projectsTruncated}
                selectedIds={selectedProjectIds}
                disabled={updating}
                onChange={setSelectedProjectIds}
              />
            )}

            {result && result.failedProjects.length === 0 && (
              <Alert
                severity="success"
                data-cy="administration-apps-organizations-enroll-result"
              >
                <T
                  keyName="administration_apps_organizations_enroll_success"
                  defaultValue="{organizationName} can now use {appName}. Enabled for {count, plural, =0 {no projects} one {# project} other {# projects}}."
                  params={{
                    organizationName: result.organizationName,
                    appName: install.name,
                    count: result.enabledCount,
                  }}
                />
              </Alert>
            )}

            {result && result.failedProjects.length > 0 && (
              <Alert
                severity="warning"
                data-cy="administration-apps-organizations-enroll-result"
              >
                <T
                  keyName="administration_apps_organizations_enroll_partial"
                  defaultValue="{organizationName} can now use {appName}, and it was enabled for {count, plural, =0 {no projects} one {# project} other {# projects}}. Enabling it failed for {failed} — the failed projects are still selected, so you can try again."
                  params={{
                    organizationName: result.organizationName,
                    appName: install.name,
                    count: result.enabledCount,
                    failed: result.failedProjects
                      .map((project) => project.name)
                      .join(', '),
                  }}
                />
              </Alert>
            )}

            <Box>
              <LoadingButton
                data-cy="administration-apps-organizations-enroll-submit"
                variant="contained"
                color="primary"
                loading={enrolling}
                disabled={!organization || updating}
                onClick={handleEnroll}
              >
                {selectedProjectIds.length === 0 ? (
                  <T
                    keyName="administration_apps_organizations_enroll_submit"
                    defaultValue="Grant access"
                  />
                ) : (
                  <T
                    keyName="administration_apps_organizations_enroll_submit_with_projects"
                    defaultValue="Grant access & enable"
                  />
                )}
              </LoadingButton>
            </Box>
          </StyledEnroll>
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
            {items.map((item) => (
              <StyledRow
                key={item.id}
                data-cy="administration-apps-organizations-item"
                data-cy-organization-id={item.id}
              >
                <Typography variant="body2">
                  {item.name} <Chip size="small" label={item.id} />
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
                        onClick={() => handleRemove(item.id, item.name)}
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
