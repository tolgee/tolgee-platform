import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Typography } from '@mui/material';

import { BaseView } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { TestClockControls } from 'tg.component/testClock/TestClockControls';

export const TestClockView: FC = () => {
  const { t } = useTranslate();
  const title = t('test_clock_title', 'Test clock');

  return (
    <DashboardPage>
      <BaseView
        windowTitle={title}
        title={title}
        maxWidth="normal"
        data-cy="test-clock-view"
      >
        <Box display="grid" gap={2}>
          <Typography variant="body2" color="text.secondary">
            {t(
              'test_clock_hint',
              'Moves the server clock for the whole instance, so you can test anything that depends on time. A moved clock stands still until you reset it.'
            )}
          </Typography>
          <TestClockControls />
        </Box>
      </BaseView>
    </DashboardPage>
  );
};
