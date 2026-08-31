import React, { useEffect, useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Alert, Box, styled } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { isRequestedProjectInaccessible } from 'tg.component/security/oauth2/consentProjectAccess';
import {
  authorizeRequestFromSearch,
  consentRequest,
} from 'tg.component/security/oauth2/oauth2ConsentSubmit';
import {
  NO_CHOICE,
  ProjectChoice,
  initialProjectChoice,
  isChoiceComplete,
} from 'tg.component/security/oauth2/consentProjectChoice';
import { ConsentProjectPicker } from 'tg.component/security/oauth2/ConsentProjectPicker';
import { ConsentPermissions } from 'tg.component/security/oauth2/ConsentPermissions';

const StyledButtons = styled(Box)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(2)};
  justify-content: flex-end;
  margin-top: ${({ theme }) => theme.spacing(3)};
`;

const OAuth2ConsentView: React.FC<React.PropsWithChildren<unknown>> = () => {
  const { t } = useTranslate();
  const [state, setState] = useState<string>();
  const [selectedScopes, setSelectedScopes] = useState<string[]>([]);
  const [projectChoice, setProjectChoice] = useState<ProjectChoice>(NO_CHOICE);
  const openedRef = useRef(false);
  const [failed, setFailed] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const authorize = useApiMutation({
    url: '/v2/oauth2/authorize',
    method: 'post',
  });
  const consent = useApiMutation({
    url: '/v2/oauth2/consent',
    method: 'post',
  });

  useEffect(() => {
    // StrictMode double-invokes the effect on the same fiber; without this the second call opens a grant nobody
    // ever sees. A genuine remount gets a fresh ref and deliberately starts a new authorization.
    if (openedRef.current) {
      return;
    }
    openedRef.current = true;
    authorize.mutate(
      {
        content: {
          'application/json': authorizeRequestFromSearch(
            window.location.search
          ),
        },
      },
      {
        onSuccess(result) {
          if (result.redirectUrl) {
            window.location.replace(result.redirectUrl);
            return;
          }
          if (!result.consentState) {
            setFailed(true);
            return;
          }
          setState(result.consentState);
        },
        onError: () => setFailed(true),
      }
    );
  }, []);

  const consentLoadable = useApiQuery({
    url: '/v2/oauth2/consent-info',
    method: 'get',
    query: { state: state! },
    options: { enabled: Boolean(state) },
  });
  const info = consentLoadable.data;

  useEffect(() => {
    if (!info) {
      return;
    }
    setSelectedScopes(info.scopes);
    setProjectChoice(initialProjectChoice(info));
  }, [info]);

  const submitDecision = (approvedScopes: string[]) => {
    setSubmitting(true);
    consent.mutate(
      {
        content: {
          'application/json': consentRequest({
            state: state!,
            approvedScopes,
            projectChoice,
          }),
        },
      },
      {
        onSuccess: (result) => window.location.replace(result.redirectUrl),
        onError: () => {
          setSubmitting(false);
          setFailed(true);
        },
      }
    );
  };

  const deny = () => submitDecision([]);
  const allow = () => submitDecision(selectedScopes);

  if (failed || consentLoadable.isError) {
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
            {isRequestedProjectInaccessible(info) && (
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
            <ConsentProjectPicker
              value={projectChoice}
              onChange={setProjectChoice}
            />
            <ConsentPermissions
              requestedScopes={info.scopes}
              requiredScopes={info.requiredScopes}
              selectedScopes={selectedScopes}
              onSelectedScopesChange={setSelectedScopes}
            />
            <StyledButtons>
              <LoadingButton
                variant="outlined"
                color="secondary"
                data-cy="oauth2-consent-deny"
                loading={submitting}
                onClick={deny}
              >
                <T keyName="oauth2_consent_deny" defaultValue="Deny" />
              </LoadingButton>
              <LoadingButton
                variant="contained"
                color="primary"
                data-cy="oauth2-consent-allow"
                loading={submitting}
                disabled={
                  selectedScopes.length === 0 ||
                  !isChoiceComplete(projectChoice)
                }
                onClick={allow}
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
