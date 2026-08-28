import React, { useEffect, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import {
  Alert,
  Box,
  Chip,
  FormControl,
  InputLabel,
  Link,
  MenuItem,
  Select,
  styled,
} from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { useScopeTranslations } from 'tg.component/PermissionsSettings/useScopeTranslations';
import {
  limitStructureToOptions,
  usePermissionsStructure,
} from 'tg.component/PermissionsSettings/usePermissionsStructure';
import { Hierarchy } from 'tg.component/PermissionsSettings/Hierarchy';
import {
  PermissionAdvancedState,
  PermissionModelScope,
} from 'tg.component/PermissionsSettings/types';
import { deriveConsentProjects } from 'tg.component/security/oauth2/consentProjectOptions';
import { groupConsentScopes } from 'tg.component/security/oauth2/consentScopeGroups';
import { clampApprovedScopes } from 'tg.component/security/oauth2/consentScopeSelection';
import {
  ALL_PROJECTS,
  selectConsentProject,
  submitConsentForm,
} from 'tg.component/security/oauth2/oauth2ConsentSubmit';

const asString = (value: string | string[] | undefined): string =>
  Array.isArray(value) ? value[0] ?? '' : value ?? '';

const StyledPermissionsHeader = styled(Box)`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: ${({ theme }) => theme.spacing(2)};
`;

const StyledSectionTitle = styled('div')`
  font-weight: 600;
`;

const StyledGroup = styled('div')`
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing(0.5)};
  margin-top: ${({ theme }) => theme.spacing(1)};
`;

const StyledGroupLabel = styled('div')`
  color: ${({ theme }) => theme.palette.text.secondary};
  margin-right: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledButtons = styled(Box)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(2)};
  justify-content: flex-end;
  margin-top: ${({ theme }) => theme.spacing(3)};
`;

const OAuth2ConsentView: React.FC<React.PropsWithChildren<unknown>> = () => {
  const { t } = useTranslate();
  const { getScopeTranslation } = useScopeTranslations();
  const structure = usePermissionsStructure();
  const search = useUrlSearch();
  const clientId = asString(search.client_id);
  const scope = asString(search.scope);
  const state = asString(search.state);
  const [selectedScopes, setSelectedScopes] = useState<string[]>([]);
  const [selectedProject, setSelectedProject] = useState<
    number | typeof ALL_PROJECTS
  >(ALL_PROJECTS);
  const [editing, setEditing] = useState(false);
  const [submitFailed, setSubmitFailed] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const consentLoadable = useApiQuery({
    url: '/v2/oauth2/consent-info',
    method: 'get',
    query: { clientId, scope, state },
  });
  const info = consentLoadable.data;
  const failed = submitFailed || consentLoadable.isError;

  useEffect(() => {
    if (!info) {
      return;
    }
    setSelectedScopes(info.scopes);
    setSelectedProject(info.project ? info.project.id : ALL_PROJECTS);
  }, [info]);

  const dependenciesLoadable = useApiQuery({
    url: '/v2/public/scope-info/hierarchy',
    method: 'get',
    query: {},
  });

  const handleScopesChange = (data: PermissionAdvancedState) => {
    setSelectedScopes(
      clampApprovedScopes(
        data.scopes as string[],
        info?.scopes ?? [],
        info?.requiredScopes ?? []
      )
    );
  };

  const submitConsent = async (approvedScopes: string[]) => {
    setSubmitting(true);
    // A denial approves no scopes, so there is no project to bind and nothing to select.
    if (approvedScopes.length > 0) {
      try {
        await selectConsentProject({ state, selectedProject });
      } catch {
        setSubmitting(false);
        setSubmitFailed(true);
        return;
      }
    }
    submitConsentForm({ clientId, state, approvedScopes });
  };

  // Approving nothing is how the authorization endpoint is told the user refused: it answers the client with
  // `access_denied`. There is no separate deny parameter in the OAuth consent form.
  const denyConsent = () => submitConsent([]);

  if (failed) {
    return (
      <DashboardPage>
        <CompactView
          windowTitle={t('oauth2_consent_title', 'Authorize application')}
          title={t('oauth2_consent_title', 'Authorize application')}
          alerts={
            <Alert severity="error" data-cy="oauth2-consent-error">
              <T
                keyName="oauth2_consent_error"
                defaultValue="Could not load the authorization request."
              />
            </Alert>
          }
          primaryContent={null}
        />
      </DashboardPage>
    );
  }

  if (!info) {
    return <FullPageLoading />;
  }

  const { requestedInaccessible, projectOptions } = deriveConsentProjects(info);
  const limitedStructure = limitStructureToOptions(
    [structure],
    info.scopes as PermissionModelScope[]
  );
  const grantedGroups = groupConsentScopes(
    info.scopes.filter((s) => selectedScopes.includes(s)),
    structure
  );

  return (
    <DashboardPage>
      <CompactView
        windowTitle={t('oauth2_consent_title', 'Authorize application')}
        title={t('oauth2_consent_heading', 'Allow access')}
        subtitle={
          <T
            keyName="oauth2_consent_subtitle"
            defaultValue="{appName} wants to access a project. Review what it will be able to do."
            params={{ appName: info.appName }}
          />
        }
        primaryContent={
          <Box data-cy="oauth2-consent">
            {requestedInaccessible && (
              <Alert
                severity="warning"
                data-cy="oauth2-consent-project-inaccessible"
                sx={{ mb: 2 }}
              >
                <T
                  keyName="oauth2_consent_project_inaccessible"
                  defaultValue="The site requested a project you can't edit on this account, so in-context editing won't work here. Sign in with an account that has access, or ask the project's team to add you."
                />
              </Alert>
            )}
            <FormControl fullWidth size="small">
              <InputLabel id="oauth2-consent-project-label">
                {t('oauth2_consent_project_label', 'Project')}
              </InputLabel>
              <Select
                labelId="oauth2-consent-project-label"
                label={t('oauth2_consent_project_label', 'Project')}
                value={selectedProject}
                data-cy="oauth2-consent-project-select"
                onChange={(e) =>
                  setSelectedProject(
                    e.target.value === ALL_PROJECTS
                      ? ALL_PROJECTS
                      : Number(e.target.value)
                  )
                }
              >
                <MenuItem
                  value={ALL_PROJECTS}
                  data-cy="oauth2-consent-project-option"
                  data-cy-project-id={ALL_PROJECTS}
                >
                  {t('oauth2_consent_all_projects_option', 'All projects')}
                </MenuItem>
                {projectOptions.map((p) => (
                  <MenuItem
                    key={p.id}
                    value={p.id}
                    data-cy="oauth2-consent-project-option"
                    data-cy-project-id={p.id}
                  >
                    {p.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <StyledPermissionsHeader>
              <StyledSectionTitle>
                <T
                  keyName="oauth2_consent_permissions_title"
                  defaultValue="Permissions"
                />
              </StyledSectionTitle>
              <Link
                component="button"
                type="button"
                data-cy="oauth2-consent-modify"
                onClick={() => setEditing((prev) => !prev)}
              >
                {editing
                  ? t('oauth2_consent_modify_done', 'Done')
                  : t('oauth2_consent_modify', 'Modify')}
              </Link>
            </StyledPermissionsHeader>

            {!editing &&
              grantedGroups.map((group, i) => (
                <StyledGroup key={group.label ?? `_${i}`}>
                  {group.label && (
                    <StyledGroupLabel>
                      <T
                        keyName="oauth2_consent_scope_group_label"
                        defaultValue="{group}:"
                        params={{ group: group.label }}
                      />
                    </StyledGroupLabel>
                  )}
                  {group.scopes.map((s) => (
                    <Chip
                      key={s}
                      size="small"
                      data-cy="oauth2-consent-scope"
                      data-cy-scope={s}
                      label={getScopeTranslation(s as PermissionModelScope)}
                    />
                  ))}
                </StyledGroup>
              ))}

            {editing &&
              (dependenciesLoadable.isLoading ? (
                <Box sx={{ mt: 1 }}>
                  <SpinnerProgress />
                </Box>
              ) : (
                <Box sx={{ mt: 1 }} data-cy="oauth2-consent-scopes">
                  {limitedStructure.map((structureItem, i) => (
                    <Hierarchy
                      key={i}
                      structure={structureItem}
                      dependencies={dependenciesLoadable.data!}
                      state={{
                        scopes: selectedScopes as PermissionModelScope[],
                      }}
                      onChange={handleScopesChange}
                      lockedScopes={
                        info.requiredScopes as PermissionModelScope[]
                      }
                    />
                  ))}
                </Box>
              ))}

            <StyledButtons>
              <LoadingButton
                variant="outlined"
                color="secondary"
                data-cy="oauth2-consent-deny"
                loading={submitting}
                onClick={denyConsent}
              >
                <T keyName="oauth2_consent_deny" defaultValue="Deny" />
              </LoadingButton>
              <LoadingButton
                variant="contained"
                color="primary"
                data-cy="oauth2-consent-allow"
                loading={submitting}
                disabled={selectedScopes.length === 0}
                onClick={() => submitConsent(selectedScopes)}
              >
                <T keyName="oauth2_consent_allow" defaultValue="Allow" />
              </LoadingButton>
            </StyledButtons>
          </Box>
        }
      />
    </DashboardPage>
  );
};

export default OAuth2ConsentView;
