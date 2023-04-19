import { components } from 'tg.service/billingApiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { TableCell } from '@mui/material';
import { ItemRow } from './ItemRow';

export const ProportionalUsageItemRow = (props: {
  item: components['schemas']['ProportionalUsageItemModel'];
  label: string;
}) => {
  const formatDate = useDateFormatter();

  const from =
    props.item.from &&
    formatDate(props.item.from, {
      timeStyle: 'short',
      dateStyle: 'short',
    });

  const to =
    props.item.to &&
    formatDate(props.item.to, {
      timeStyle: 'short',
      dateStyle: 'short',
    });

  return (
    <ItemRow
      label={props.label}
      item={props.item}
      periodInfo={
        <>
          <TableCell>{from}</TableCell>
          <TableCell>{to}</TableCell>
        </>
      }
    />
  );
};
