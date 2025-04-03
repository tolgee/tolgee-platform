import { components } from 'tg.service/billingApiSchema.generated';
import { ItemRow } from './ItemRow';

export const SumUsageItemRow = (props: {
  item: components['schemas']['SumUsageItemModel'];
  label: string;
  dataCy?: string;
}) => {
  return (
    <ItemRow
      label={props.label}
      item={props.item}
      tableRowProps={{ 'data-cy': props.dataCy } as any}
    />
  );
};
