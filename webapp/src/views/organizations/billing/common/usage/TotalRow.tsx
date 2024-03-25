import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { useMoneyFormatter } from 'tg.hooks/useLocale';
import { TableCell, TableRow } from '@mui/material';

export const TotalRow: FC<{ total: number; appliedStripeCredits: number }> = ({
  total,
  appliedStripeCredits,
}) => {
  const { t } = useTranslate();

  const formatMoney = useMoneyFormatter();

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
        <b>{formatMoney(total - appliedStripeCredits)}</b>
      </TableCell>
    </TableRow>
  );
};
