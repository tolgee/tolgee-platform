import React, { useEffect, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Alert } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import { apiV2HttpService } from 'tg.service/http/ApiV2HttpService';
import { isSafeContinue } from 'tg.component/security/oauth2/oauth2Continue';

const API_URL = import.meta.env.VITE_APP_API_URL || '';

const OAuth2BootstrapView: React.FC<React.PropsWithChildren<unknown>> = () => {
  const { t } = useTranslate();
  const search = useUrlSearch();
  const continueUrl =
    typeof search.continue === 'string' ? search.continue : undefined;
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    if (!isSafeContinue(continueUrl, API_URL)) {
      setFailed(true);
      return;
    }
    apiV2HttpService
      .post('oauth2/session-bootstrap', {})
      .then(() => {
        window.location.href = continueUrl;
      })
      .catch(() => setFailed(true));
  }, [continueUrl]);

  if (!failed) {
    return <FullPageLoading />;
  }

  return (
    <DashboardPage>
      <CompactView
        windowTitle={t('oauth2_bootstrap_title', 'Connecting')}
        title={t('oauth2_bootstrap_title', 'Connecting')}
        alerts={
          <Alert severity="error" data-cy="oauth2-bootstrap-error">
            <T
              keyName="oauth2_bootstrap_error"
              defaultValue="Could not start the authorization. Please try connecting again from the application."
            />
          </Alert>
        }
        primaryContent={null}
      />
    </DashboardPage>
  );
};

export default OAuth2BootstrapView;
