import { Box, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { BillingProgress } from './BillingProgress';

type Props = {
  translationsAvailable: number;
  translationsMax: number;
  creditAvailable: number;
  creditMax: number;
};

export const UsageDetailed: React.FC<Props> = ({
  translationsAvailable,
  translationsMax,
  creditAvailable,
  creditMax,
}) => {
  const translationsProgress = (translationsAvailable / translationsMax) * 100;
  const creditsProgress = (creditAvailable / creditMax) * 100;

  return (
    <Box display="grid" gap={1}>
      <Box>
        <Typography variant="caption">
          <T
            keyName="dashboard_billing_translations"
            parameters={{
              available: Math.round(translationsAvailable),
              max: Math.round(translationsMax),
            }}
          />
        </Typography>
        <BillingProgress percent={translationsProgress} />
      </Box>
      <Box>
        <Typography variant="caption">
          <T
            keyName="dashboard_billing_credit"
            parameters={{
              available: Math.round(creditAvailable / 100),
              max: Math.round(creditMax / 100),
            }}
          />
        </Typography>
        <BillingProgress percent={creditsProgress} />
      </Box>
    </Box>
  );
};
