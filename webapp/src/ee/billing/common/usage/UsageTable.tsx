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
import { CarryOverRow } from './CarryOverRow';

export const UsageTable: FC<{
  usageData: components['schemas']['UsageModel'];
  invoiceId?: number;
  invoiceNumber?: string;
  organizationId?: number;
}> = ({ usageData, invoiceId, invoiceNumber, organizationId }) => {
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
            organizationId={organizationId}
            item={usageData.seats}
          />
        )}

        {usageData?.translations.total.valueOf() > 0 && (
          <ProportionalUsageItemRow
            type="TRANSLATIONS"
            invoiceId={invoiceId}
            invoiceNumber={invoiceNumber}
            organizationId={organizationId}
            item={usageData.translations}
          />
        )}

        {usageData?.keys.total.valueOf() > 0 && (
          <ProportionalUsageItemRow
            type="KEYS"
            invoiceId={invoiceId}
            invoiceNumber={invoiceNumber}
            organizationId={organizationId}
            item={usageData.keys}
          />
        )}

        {(usageData?.credits?.usedQuantity?.valueOf() || 0) > 0 && (
          <SumUsageItemRow
            dataCy="billing-usage-table-credits"
            item={usageData!.credits!}
            label={t('invoice_usage_dialog_table_mt_credits_item')}
          />
        )}
        {(usageData?.appliedStripeCredits || 0) > 0 && (
          <AppliedStripeCreditsRow value={usageData.appliedStripeCredits!} />
        )}

        {(usageData?.carryOverTotal || 0) > 0 && (
          <CarryOverRow value={usageData.carryOverTotal!} />
        )}

        <TotalRow
          total={usageData.total}
          appliedStripeCredits={usageData.appliedStripeCredits || 0}
          subscriptionPrice={usageData.subscriptionPrice}
        />
      </TableBody>
    </Table>
  );
};
