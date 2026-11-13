import { FC } from 'react';
import { useTestClock } from 'tg.service/useTestClock';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { Box, Button, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';

export const TopBarTestClockInfo: FC<React.PropsWithChildren<unknown>> = () => {
  const testClock = useTestClock();

  const formatDate = useDateFormatter();

  if (!testClock) {
    return null;
  }

  return (
    <Tooltip title={<T keyName="development-test-clock-topbar-info-tooltip" />}>
      <Button
        component={Link}
        to={LINKS.TEST_CLOCK.build()}
        data-cy="top-bar-test-clock"
        sx={(theme) => ({
          p: 0,
          mr: 1,
          mx: 0,
          display: 'flex',
          alignItems: 'center',
          flexDirection: 'column',
          boxShadow: `0px 0px 10px ${theme.palette.colors.red['100']}`,
        })}
      >
        <Box sx={{ fontSize: 14 }}>
          {formatDate(testClock, { dateStyle: 'short' })}
        </Box>
        <Box sx={{ fontSize: 12, mt: '-10px' }}>
          {formatDate(testClock, { timeStyle: 'short' })}
        </Box>
      </Button>
    </Tooltip>
  );
};
