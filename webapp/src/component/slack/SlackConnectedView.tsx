import { Alert, Box } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';

export const SlackConnectedView = () => {
  const { t } = useTranslate();
  return (
    <DashboardPage>
      <CompactView
        maxWidth={700}
        alerts={
          <Alert severity="success" variant="filled">
            <T keyName="slack_connect_alert_success" />
          </Alert>
        }
        windowTitle={t('slack_connect_title')}
        title={t('slack_connect_success_title')}
        primaryContent={
          <Box mt={4}>
            <T keyName="slack_connect_success_message" />
          </Box>
        }
      />
    </DashboardPage>
  );
};

export default SlackConnectedView;
