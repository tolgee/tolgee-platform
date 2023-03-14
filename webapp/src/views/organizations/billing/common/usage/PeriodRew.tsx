import { components } from 'tg.service/billingApiSchema.generated';
import { useDateFormatter, useMoneyFormatter } from 'tg.hooks/useLocale';
import { useTranslate } from '@tolgee/react';
import { TableCell, TableRow } from '@mui/material';

export const PeriodRew = (props: {
  period: components['schemas']['UsageItemModel'];
}) => {
  const formatDate = useDateFormatter();
  const formatMoney = useMoneyFormatter();

  const { t } = useTranslate();

  return (
    <TableRow>
      <TableCell>{t('invoice_usage_dialog_table_seats_item')}</TableCell>
      <TableCell>
        {formatDate(props.period.from, {
          timeStyle: 'short',
          dateStyle: 'short',
        })}
      </TableCell>
      <TableCell>
        {formatDate(props.period.to, {
          timeStyle: 'short',
          dateStyle: 'short',
        })}
      </TableCell>
      <TableCell align="right">{props.period.usedQuantity}</TableCell>
      <TableCell align="right">{props.period.usedQuantityOverPlan}</TableCell>
      <TableCell align="right">{formatMoney(props.period.total)}</TableCell>
    </TableRow>
  );
};
