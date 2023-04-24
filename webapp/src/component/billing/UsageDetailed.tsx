import { Box, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { BillingProgress } from './BillingProgress';

type Props = {
  translationsUsed: number;
  translationsMax: number;
  creditUsed: number;
  creditMax: number;
  isPayAsYouGo: boolean;
};

export const UsageDetailed: React.FC<Props> = ({
  translationsUsed,
  translationsMax,
  creditUsed,
  creditMax,
  isPayAsYouGo,
}) => {
  const translationsProgress = (translationsUsed / translationsMax) * 100;
  const creditsProgress = (creditUsed / creditMax) * 100;

  return (
    <Box display="grid" gap={1}>
      <Box>
        <Typography variant="caption">
          <T
            keyName="dashboard_billing_used_translations"
            params={{
              available: Math.round(translationsUsed),
              max: Math.round(translationsMax),
            }}
          />
        </Typography>
        <BillingProgress
          percent={translationsProgress}
          canGoOver={isPayAsYouGo}
        />
      </Box>
      <Box>
        <Typography variant="caption">
          <T
            keyName="dashboard_billing_used_credit"
            params={{
              available: Math.round(creditUsed / 100),
              max: Math.round(creditMax / 100),
            }}
          />
        </Typography>
        <BillingProgress percent={creditsProgress} canGoOver={isPayAsYouGo} />
      </Box>
    </Box>
  );
};
