import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { Table, TableBody } from '@mui/material';
import { UsageTableHead } from './UsageTableHead';
import { SubscriptionRow } from './SubscriptionRow';
import { ProportionalUsageItemRow } from './ProportionalUsageItemRow';
import { SumUsageItemRow } from './SumUsageItemRow';
import { TotalRow } from './TotalRow';
import { AppliedStripeCreditsRow } from './AppliedStripeCreditsRow';

export const UsageTable: FC<{
  usageData: components['schemas']['UsageModel'];
  invoiceId?: number;
  invoiceNumber?: string;
}> = ({ usageData, invoiceId, invoiceNumber }) => {
  const { t } = useTranslate();

  return (
    <Table data-cy="billing-usage-table">
      <UsageTableHead />
      <TableBody>
        <SubscriptionRow price={usageData?.subscriptionPrice} />

        {usageData?.seats.total.valueOf() > 0 && (
          <ProportionalUsageItemRow
            type="SEATS"
            invoiceId={invoiceId}
            invoiceNumber={invoiceNumber}
            label={t('invoice_usage_dialog_table_seats_item')}
            item={usageData.seats}
          />
        )}

        {usageData?.translations.total.valueOf() > 0 && (
          <ProportionalUsageItemRow
            type="TRANSLATIONS"
            invoiceId={invoiceId}
            invoiceNumber={invoiceNumber}
            label={t('invoice_usage_dialog_table_translations_item')}
            item={usageData.translations}
          />
        )}

        {(usageData?.credits?.usedQuantity?.valueOf() || 0) > 0 && (
          <SumUsageItemRow
            item={usageData!.credits!}
            label={t('invoice_usage_dialog_table_mt_credits_item')}
          />
        )}
        {(usageData?.appliedStripeCredits || 0) > 0 && (
          <AppliedStripeCreditsRow value={usageData.appliedStripeCredits!} />
        )}

        <TotalRow
          total={usageData.total}
          appliedStripeCredits={usageData.appliedStripeCredits || 0}
        />
      </TableBody>
    </Table>
  );
};
