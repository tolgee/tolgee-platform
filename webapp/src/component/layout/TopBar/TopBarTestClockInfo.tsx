import { FC } from 'react';
import { useTestClock } from 'tg.service/useTestClock';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { Box, Button, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';

export const TopBarTestClockInfo: FC = () => {
  const testClock = useTestClock();

  const formatDate = useDateFormatter();

  const organization = usePreferredOrganization()?.preferredOrganization;

  if (!testClock || !organization) {
    return null;
  }

  return (
    <Tooltip title={<T keyName="development-test-clock-topbar-info-tooltip" />}>
      <Button
        component={Link}
        to={LINKS.ORGANIZATION_BILLING_TEST_CLOCK_HELPER.build({
          [PARAMS.ORGANIZATION_SLUG]: organization.slug,
        })}
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
