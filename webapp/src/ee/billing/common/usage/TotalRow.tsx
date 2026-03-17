import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { useMoneyFormatter } from 'tg.hooks/useLocale';
import { TableCell, TableRow } from '@mui/material';
import { LabelHint } from 'tg.component/common/LabelHint';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const TotalRow: FC<{
  total: number;
  appliedStripeCredits: number;
  usageOnlyTotal?: number;
}> = ({ total, appliedStripeCredits, usageOnlyTotal }) => {
  const { t } = useTranslate();

  const formatMoney = useMoneyFormatter();

  const minUsageInvoiceAmount = useGlobalContext(
    (c) => c.initialData.serverConfiguration.billing.minUsageInvoiceAmount
  );

  const showHint = Boolean(
    minUsageInvoiceAmount &&
      usageOnlyTotal &&
      usageOnlyTotal > 0 &&
      usageOnlyTotal < minUsageInvoiceAmount &&
      total < minUsageInvoiceAmount
  );

  const totalFormatted = <b>{formatMoney(total - appliedStripeCredits)}</b>;

  return (
    <TableRow>
      <TableCell
        colSpan={2}
        sx={{
          borderBottom: 'none',
        }}
      >
        <b>{t('invoice_usage_dialog_table_total')}</b>
      </TableCell>
      <TableCell
        align="right"
        sx={{
          borderBottom: 'none',
        }}
      >
        {showHint ? (
          <LabelHint
            title={t(
              'invoice_usage_below_threshold_notice',
              'Your current usage ({usageAmount}) is below the invoicing minimum of {threshold}. It will be carried over and billed once accumulated usage reaches the minimum.',
              {
                usageAmount: formatMoney(usageOnlyTotal!),
                threshold: formatMoney(minUsageInvoiceAmount!),
              }
            )}
          >
            {totalFormatted}
          </LabelHint>
        ) : (
          totalFormatted
        )}
      </TableCell>
    </TableRow>
  );
};
