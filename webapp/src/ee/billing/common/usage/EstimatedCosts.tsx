import { UseQueryResult } from 'react-query';
import { components } from 'tg.service/billingApiSchema.generated';
import { FC, useState } from 'react';
import { useMoneyFormatter } from 'tg.hooks/useLocale';
import { useTranslate } from '@tolgee/react';
import { Box, Tooltip } from '@mui/material';
import { UsageDialogButton } from './UsageDialogButton';

export type EstimatedCostsProps = {
  useUsage: (
    enabled: boolean
  ) => UseQueryResult<components['schemas']['UsageModel']>;
  estimatedCosts?: number;
};

export const EstimatedCosts: FC<EstimatedCostsProps> = ({
  useUsage,
  estimatedCosts,
}) => {
  const formatMoney = useMoneyFormatter();

  const { t } = useTranslate();

  const [open, setOpen] = useState(false);

  const usage = useUsage(open);

  return (
    <Box
      display="flex"
      justifyContent="right"
      data-cy="billing-estimated-costs"
    >
      <Box>
        <Tooltip
          title={t('active-plan-estimated-costs-description')}
          disableInteractive
        >
          <Box>{t('active-plan-estimated-costs-title')}</Box>
        </Tooltip>
        <Box textAlign="right" display="flex" alignItems="center">
          {formatMoney(estimatedCosts || 0)}
          <UsageDialogButton
            usageData={usage.data}
            loading={usage.isLoading}
            onOpen={() => setOpen(true)}
            onClose={() => setOpen(false)}
            open={open}
          />
        </Box>
      </Box>
    </Box>
  );
};
