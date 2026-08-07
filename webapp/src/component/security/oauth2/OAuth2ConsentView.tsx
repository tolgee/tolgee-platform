import React, { useEffect, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import {
  Alert,
  Box,
  Checkbox,
  FormControlLabel,
  styled,
  Typography,
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

type ConsentInfo = {
  appName: string;
  scopes: string[];
  project?: { id: number; name: string } | null;
  allProjects: boolean;
};

const StyledCapabilities = styled('div')`
  display: grid;
  gap: 4px;
`;

const StyledButtons = styled(Box)`
  display: flex;
  gap: 16px;
  justify-content: flex-end;
  margin-top: 24px;
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
  const [failed, setFailed] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    apiV2HttpService
      .get<ConsentInfo>('oauth2/consent-info', { clientId, scope, state })
      .then((data) => {
        setInfo(data);
        setSelectedScopes(data.scopes);
      })
      .catch(() => setFailed(true));
  }, [clientId, scope, state]);

  const toggleScope = (s: string) => {
    setSelectedScopes((prev) =>
      prev.includes(s) ? prev.filter((x) => x !== s) : [...prev, s]
    );
  };

  // Real form POST (not fetch) so the browser sends the session cookie and follows the redirect back to the client.
  const submitConsent = (approvedScopes: string[]) => {
    setSubmitting(true);
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
            {info.project && (
              <Typography sx={{ mt: 2 }} data-cy="oauth2-consent-project">
                <T
                  keyName="oauth2_consent_project"
                  defaultValue="On project: {projectName}"
                  params={{ projectName: info.project.name }}
                />
              </Typography>
            )}
            {!info.project && info.allProjects && (
              <Typography sx={{ mt: 2 }} data-cy="oauth2-consent-all-projects">
                <T
                  keyName="oauth2_consent_all_projects"
                  defaultValue="On all your projects"
                />
              </Typography>
            )}
            {!info.project && !info.allProjects && (
              <Typography
                sx={{ mt: 2 }}
                data-cy="oauth2-consent-single-project"
              >
                <T
                  keyName="oauth2_consent_single_project"
                  defaultValue="On a single project"
                />
              </Typography>
            )}
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
