import { useTranslate } from '@tolgee/react';
import { TableCell, TableRow } from '@mui/material';
import { useMoneyFormatter } from 'tg.hooks/useLocale';

export const CarryOverRow = (props: { value: number }) => {
  const { t } = useTranslate();
  const formatMoney = useMoneyFormatter();

  return (
    <TableRow>
      <TableCell colSpan={2}>
        {t(
          'invoice_usage_dialog_table_carry_over_item',
          'Deferred from previous periods'
        )}
      </TableCell>
      <TableCell align="right">{formatMoney(props.value)}</TableCell>
    </TableRow>
  );
};
