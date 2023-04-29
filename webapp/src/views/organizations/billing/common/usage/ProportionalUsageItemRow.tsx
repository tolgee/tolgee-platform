import { components } from 'tg.service/billingApiSchema.generated';
import { ItemRow } from './ItemRow';

export const ProportionalUsageItemRow = (props: {
  item: components['schemas']['AverageProportionalUsageItemModel'];
  label: string;
  invoiceId?: number;
}) => {
  return <ItemRow label={props.label} item={props.item} />;
};
