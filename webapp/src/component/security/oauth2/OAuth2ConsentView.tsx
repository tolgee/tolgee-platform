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
import { apiV2HttpService } from 'tg.service/http/ApiV2HttpService';
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
import { deriveConsentProjects } from './consentProjectOptions';
import { groupConsentScopes } from './consentScopeGroups';

const API_URL = import.meta.env.VITE_APP_API_URL || '';

const asString = (value: string | string[] | undefined): string =>
  Array.isArray(value) ? value[0] ?? '' : value ?? '';

type ProjectOption = { id: number; name: string };

type ConsentInfo = {
  appName: string;
  scopes: string[];
  requiredScopes: string[];
  project?: ProjectOption | null;
  requestedProjectId?: number | null;
};

const ALL_PROJECTS = 'all' as const;

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
  const [info, setInfo] = useState<ConsentInfo>();
  const [selectedScopes, setSelectedScopes] = useState<string[]>([]);
  const [selectedProject, setSelectedProject] = useState<
    number | typeof ALL_PROJECTS
  >(ALL_PROJECTS);
  const [editing, setEditing] = useState(false);
  const [failed, setFailed] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    apiV2HttpService
      .get<ConsentInfo>('oauth2/consent-info', { clientId, scope, state })
      .then((data) => {
        setInfo(data);
        setSelectedScopes(data.scopes);
        setSelectedProject(data.project ? data.project.id : ALL_PROJECTS);
      })
      .catch(() => setFailed(true));
  }, [clientId, scope, state]);

  const dependenciesLoadable = useApiQuery({
    url: '/v2/public/scope-info/hierarchy',
    method: 'get',
    query: {},
  });

  // Keep only app-requested scopes and always re-add the required ones, so the tree can never grant beyond the
  // authorization request nor drop a locked-on scope via a parent-group toggle.
  const handleScopesChange = (data: PermissionAdvancedState) => {
    const next = new Set(data.scopes as string[]);
    info?.requiredScopes.forEach((s) => next.add(s));
    setSelectedScopes((info?.scopes ?? []).filter((s) => next.has(s)));
  };

  // Real form POST (not fetch) so the browser sends the session cookie and follows the redirect back to the client.
  const submitForm = (approvedScopes: string[]) => {
    const form = document.createElement('form');
    form.method = 'post';
    form.action = `${API_URL}/oauth2/authorize`;
    const addField = (name: string, value: string) => {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = name;
      input.value = value;
      form.appendChild(input);
    };
    addField('client_id', clientId);
    addField('state', state);
    approvedScopes.forEach((s) => addField('scope', s));
    document.body.appendChild(form);
    form.submit();
  };

  const submitConsent = async (approvedScopes: string[]) => {
    setSubmitting(true);
    // select-project must run before the SAS consent form POST, so the chosen project is on the authorization when
    // the code is issued. Denials (no scopes) skip it.
    if (approvedScopes.length > 0) {
      const projectQuery =
        selectedProject === ALL_PROJECTS ? '' : `&projectId=${selectedProject}`;
      try {
        await apiV2HttpService.post(
          `oauth2/select-project?state=${encodeURIComponent(
            state
          )}${projectQuery}`,
          {}
        );
      } catch {
        setSubmitting(false);
        setFailed(true);
        return;
      }
    }
    submitForm(approvedScopes);
  };

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
                    <StyledGroupLabel>{group.label}:</StyledGroupLabel>
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
                      disabledScopes={
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
                onClick={() => submitConsent([])}
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
