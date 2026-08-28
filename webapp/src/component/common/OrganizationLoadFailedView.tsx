import { Box } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { CompactView } from 'tg.component/layout/CompactView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';

export const OrganizationLoadFailedView = () => {
  const { t } = useTranslate();

  return (
    <DashboardPage>
      <CompactView
        primaryContent={
          <Box data-cy="organization-load-failed-message">
            <T
              keyName="organization_load_failed_message"
              defaultValue="This organization could not be loaded. Please refresh the page to try again."
            />
          </Box>
        }
        title={
          <T
            keyName="organization_load_failed_title"
            defaultValue="Could not load organization"
          />
        }
        windowTitle={t(
          'organization_load_failed_title',
          'Could not load organization'
        )}
      />
    </DashboardPage>
  );
};
