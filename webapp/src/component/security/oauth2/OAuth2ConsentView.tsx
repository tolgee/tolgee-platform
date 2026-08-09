import React, { useEffect, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import {
  Alert,
  Box,
  Checkbox,
  FormControl,
  FormControlLabel,
  InputLabel,
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
import { useScopeTranslations } from 'tg.component/PermissionsSettings/useScopeTranslations';
import { PermissionModelScope } from 'tg.component/PermissionsSettings/types';

const API_URL = import.meta.env.VITE_APP_API_URL || '';

const asString = (value: string | string[] | undefined): string =>
  Array.isArray(value) ? value[0] ?? '' : value ?? '';

type ProjectOption = { id: number; name: string };

type ConsentInfo = {
  appName: string;
  scopes: string[];
  project?: ProjectOption | null;
  projects: ProjectOption[];
};

const ALL_PROJECTS = 'all' as const;

const StyledCapabilities = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
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
  const search = useUrlSearch();
  const clientId = asString(search.client_id);
  const scope = asString(search.scope);
  const state = asString(search.state);
  const [info, setInfo] = useState<ConsentInfo>();
  const [selectedScopes, setSelectedScopes] = useState<string[]>([]);
  const [selectedProject, setSelectedProject] = useState<
    number | typeof ALL_PROJECTS
  >(ALL_PROJECTS);
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

  const toggleScope = (s: string) => {
    setSelectedScopes((prev) =>
      prev.includes(s) ? prev.filter((x) => x !== s) : [...prev, s]
    );
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

  const hintedProject = info.project;
  // A hinted project may be a public one the user isn't a member of — keep it selectable even if not in the list.
  const projectOptions =
    hintedProject && !info.projects.some((p) => p.id === hintedProject.id)
      ? [hintedProject, ...info.projects]
      : info.projects;

  return (
    <DashboardPage>
      <CompactView
        windowTitle={t('oauth2_consent_title', 'Authorize application')}
        title={t('oauth2_consent_title', 'Authorize application')}
        subtitle={
          <T
            keyName="oauth2_consent_subtitle"
            defaultValue="{appName} is requesting permission to:"
            params={{ appName: info.appName }}
          />
        }
        primaryContent={
          <Box data-cy="oauth2-consent">
            <StyledCapabilities>
              {info.scopes.map((s) => (
                <FormControlLabel
                  key={s}
                  data-cy="oauth2-consent-scope"
                  data-cy-scope={s}
                  control={
                    <Checkbox
                      size="small"
                      checked={selectedScopes.includes(s)}
                      onChange={() => toggleScope(s)}
                    />
                  }
                  label={getScopeTranslation(s as PermissionModelScope)}
                />
              ))}
            </StyledCapabilities>
            <FormControl fullWidth size="small" sx={{ mt: 2 }}>
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
