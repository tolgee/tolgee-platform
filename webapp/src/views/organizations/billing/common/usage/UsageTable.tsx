import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { Table, TableBody } from '@mui/material';
import { UsageTableHead } from './UsageTableHead';
import { SubscriptionRow } from './SubscriptionRow';
import { ProportionalUsageItemRow } from './ProportionalUsageItemRow';
import { SumUsageItemRow } from './SumUsageItemRow';
import { TotalRow } from './TotalRow';

export const UsageTable: FC<{
  usageData: components['schemas']['MeteredUsageModel'];
}> = ({ usageData }) => {
  const { t } = useTranslate();

  return (
    <Table>
      <UsageTableHead />
      <TableBody>
        <SubscriptionRow price={usageData?.subscriptionPrice} />
        {(usageData?.seatsPeriods || []).map((period) => (
          <ProportionalUsageItemRow
            label={t('invoice_usage_dialog_table_seats_item')}
            key={period.from}
            item={period}
          />
        ))}

        {(usageData?.translationsPeriods || []).map((period) => (
          <ProportionalUsageItemRow
            label={t('invoice_usage_dialog_table_translations_item')}
            key={period.from}
            item={period}
          />
        ))}

        {usageData?.credits && (
          <SumUsageItemRow
            item={usageData?.credits}
            label={t('invoice_usage_dialog_table_mt_credits_item')}
          />
        )}
        <TotalRow total={usageData.total} />
      </TableBody>
    </Table>
  );
};
