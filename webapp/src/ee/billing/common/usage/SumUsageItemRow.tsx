import { components } from 'tg.service/billingApiSchema.generated';
import { ItemRow } from './ItemRow';

export const SumUsageItemRow = (props: {
  item: components['schemas']['SumUsageItemModel'];
  label: string;
}) => {
  return <ItemRow label={props.label} item={props.item} />;
};
