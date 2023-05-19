import { Box, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { BillingProgress } from './BillingProgress';
import { ProgressData } from './utils';

type Props = ProgressData;

export const UsageDetailed: React.FC<Props> = ({
  translationsUsed,
  translationsMax,
  creditUsed,
  creditMax,
  isPayAsYouGo,
  usesSlots,
}) => {
  return (
    <Box display="grid" gap={1}>
      <Box>
        <Typography variant="caption">
          {usesSlots ? (
            <T
              keyName="dashboard_billing_used_translations"
              params={{
                available: Math.round(translationsUsed),
                max: Math.round(translationsMax),
              }}
            />
          ) : (
            <T
              keyName="dashboard_billing_used_strings"
              params={{
                available: Math.round(translationsUsed),
                max: Math.round(translationsMax),
              }}
            />
          )}
        </Typography>
        <BillingProgress
          value={translationsUsed}
          maxValue={translationsMax}
          canGoOver={isPayAsYouGo}
        />
      </Box>
      <Box>
        <Typography variant="caption">
          <T
            keyName="dashboard_billing_used_credit"
            params={{
              available: Math.round(creditUsed),
              max: Math.round(creditMax),
            }}
          />
        </Typography>
        <BillingProgress
          value={creditUsed}
          maxValue={creditMax}
          canGoOver={isPayAsYouGo}
        />
      </Box>
    </Box>
  );
};
