import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { useMoneyFormatter, useNumberFormatter } from 'tg.hooks/useLocale';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material';

export const TotalTable: FC<{
  invoice: components['schemas']['InvoiceModel'];
  totalWithoutVat?: number;
  appliedStripeCredits?: number;
}> = ({ invoice, totalWithoutVat, appliedStripeCredits }) => {
  const { t } = useTranslate();
  const formatMoney = useMoneyFormatter();

  const formatNumber = useNumberFormatter();

  const totalWithAppliedCredits = Number(
    ((totalWithoutVat || 0) - (appliedStripeCredits || 0)).toFixed(2)
  );

  const vat = Number(
    (
      (invoice.taxRatePercentage &&
        totalWithAppliedCredits &&
        (invoice.taxRatePercentage / 100) * (totalWithAppliedCredits || 0)) ||
      0
    ).toFixed(2)
  );

  const total = vat + (totalWithAppliedCredits || 0);

  return (
    <Table>
      <TableHead>
        <TableRow>
          <TableCell align="right">
            {t('invoice_usage_dialog_table_vat_rate')}
          </TableCell>
          <TableCell align="right">
            {t('invoice_usage_dialog_table_vat')}
          </TableCell>
        </TableRow>
      </TableHead>

      <TableBody>
        <TableRow>
          <TableCell align="right">
            {formatNumber((invoice.taxRatePercentage || 0) / 100, {
              style: 'percent',
              minimumFractionDigits: 1,
              maximumFractionDigits: 1,
            })}
          </TableCell>
          <TableCell align="right">{formatMoney(vat)}</TableCell>
        </TableRow>
        <TableRow>
          <TableCell
            align="right"
            colSpan={2}
            sx={{
              borderBottom: 'none',
              fontWeight: 'bold',
              fontSize: '1.2em',
            }}
          >
            {formatMoney(total)}
          </TableCell>
        </TableRow>
      </TableBody>
    </Table>
  );
};
