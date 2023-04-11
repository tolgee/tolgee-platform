import { FC, useState } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useMoneyFormatter } from 'tg.hooks/useLocale';
import { useTranslate } from '@tolgee/react';
import { PlanEstimatedCostsArea } from '../common/Plan';
import {
  Box,
  DialogContent,
  DialogTitle,
  IconButton,
  Table,
  TableBody,
  Tooltip,
  Typography,
} from '@mui/material';
import { DataUsage } from '@mui/icons-material';
import { UsageTableHead } from '../../common/usage/UsageTableHead';
import { SubscriptionRow } from '../../common/usage/SubscriptionRow';
import { PeriodRew } from '../../common/usage/PeriodRew';
import { TotalRow } from '../../common/usage/TotalRow';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import Dialog from '@mui/material/Dialog';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from '../../../useOrganization';

export const SelfHostedEeEstimatedCosts: FC<{
  subscription: components['schemas']['SelfHostedEeSubscriptionModel'];
}> = ({ subscription }) => {
  const formatMoney = useMoneyFormatter();

  const { t } = useTranslate();

  const [open, setOpen] = useState(false);

  const organization = useOrganization();

  const usage = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/self-hosted-ee/subscriptions/{subscriptionId}/expected-usage',
    method: 'get',
    path: {
      organizationId: organization!.id,
      subscriptionId: subscription.id,
    },
    options: {
      enabled: open,
    },
  });

  return (
    <PlanEstimatedCostsArea>
      <Box display="flex" justifyContent="right">
        <Box>
          <Tooltip title={t('active-plan-estimated-costs-description')}>
            <Typography variant="caption">
              {t('active-plan-estimated-costs-title')}
            </Typography>
          </Tooltip>
          <Box textAlign="right" display="flex" alignItems="center">
            {formatMoney(subscription.estimatedCosts || 0)}
            <Tooltip
              title={t('active-plan-estimated-costs-show-usage-button-tooltip')}
            >
              <IconButton size="small" onClick={() => setOpen(true)}>
                <DataUsage />
              </IconButton>
            </Tooltip>
          </Box>
        </Box>
      </Box>
      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md">
        <DialogTitle>{t('invoice_usage_dialog_title')}</DialogTitle>
        <DialogContent>
          {usage.data ? (
            <>
              <Table>
                <UsageTableHead />
                <TableBody>
                  <SubscriptionRow price={usage.data?.subscriptionPrice} />
                  {(usage.data?.periods || []).map((period) => (
                    <PeriodRew key={period.from} period={period} />
                  ))}
                  <TotalRow total={usage.data.total} />
                </TableBody>
              </Table>
            </>
          ) : (
            <EmptyListMessage loading={usage.isLoading} />
          )}
        </DialogContent>
      </Dialog>
    </PlanEstimatedCostsArea>
  );
};
