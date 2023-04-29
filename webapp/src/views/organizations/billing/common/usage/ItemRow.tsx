import { components } from 'tg.service/billingApiSchema.generated';
import { useMoneyFormatter } from 'tg.hooks/useLocale';
import { TableCell, TableRow } from '@mui/material';

export const ItemRow = (props: {
  item:
    | components['schemas']['AverageProportionalUsageItemModel']
    | components['schemas']['SumUsageItemModel'];
  label: string;
}) => {
  const formatMoney = useMoneyFormatter();
  return (
    <TableRow>
      <TableCell>{props.label}</TableCell>
      <TableCell align="right">{props.item.usedQuantity}</TableCell>
      <TableCell align="right">{props.item.usedQuantityOverPlan}</TableCell>
      <TableCell align="right">{formatMoney(props.item.total)}</TableCell>
    </TableRow>
  );
};
