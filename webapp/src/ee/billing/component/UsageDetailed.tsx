import { Box, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { BillingProgress } from './BillingProgress';
import { ProgressData } from './getProgressData';

export const UsageDetailed: React.FC<
  Partial<ProgressData> & { isPayAsYouGo: boolean }
> = (props) => {
  const items = [
    {
      getLabel: (params: { limit: number; used: number }) => (
        <T keyName="dashboard_billing_used_strings_v2" params={params} />
      ),
      progress: props.stringsProgress,
    },
    {
      getLabel: (params: { limit: number; used: number }) => (
        <T keyName="dashboard_billing_used_seats" params={params} />
      ),
      progress: props.seatsProgress,
    },
    {
      getLabel: (params: { limit: number; used: number }) => (
        <T keyName="dashboard_billing_used_keys" params={params} />
      ),
      progress: props.keysProgress,
    },
    {
      getLabel: (params: { limit: number; used: number }) => (
        <T keyName="dashboard_billing_used_credit_v2" params={params} />
      ),
      progress: props.creditProgress,
    },
  ];

  return (
    <Box display="grid" gap={1}>
      {items.map((item, index) => {
        if (!item.progress?.isInUse) {
          return null;
        }

        return (
          <Box key={index}>
            <Typography variant="caption">
              {item.getLabel({
                limit: Math.round(item.progress.included),
                used: Math.round(item.progress.used),
              })}
            </Typography>
            <BillingProgress
              progressItem={item.progress}
              isPayAsYouGo={props.isPayAsYouGo}
            />
          </Box>
        );
      })}
    </Box>
  );
};
