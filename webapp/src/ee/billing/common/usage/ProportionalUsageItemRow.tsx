import { components, operations } from 'tg.service/billingApiSchema.generated';
import { ItemRow } from './ItemRow';
import { useTranslate } from '@tolgee/react';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';

export type ProportionalUsageType =
  operations['getUsageDetail']['parameters']['path']['type'];

export const ProportionalUsageItemRow = (props: {
  item: components['schemas']['AverageProportionalUsageItemModel'];
  invoiceId?: number;
  invoiceNumber?: string;
  type: ProportionalUsageType;
}) => {
  const downloadReport = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/invoices/{invoiceId}/usage/{type}.csv',
    method: 'get',
    fetchOptions: { rawResponse: true },
  });

  const organization = useOrganization();

  const onDownload =
    (props.invoiceId !== undefined &&
      (() => {
        downloadReport.mutate(
          {
            path: {
              organizationId: organization!.id,
              invoiceId: props.invoiceId!,
              type: props.type,
            },
          },
          {
            onSuccess: async (response) => {
              const data = (await (response as any).blob()) as Blob;
              const fileUrl = URL.createObjectURL(data);
              const filename = `Report - ${props.invoiceNumber} - ${props.type}.csv`;
              const a = document.createElement('a');
              a.href = fileUrl;
              a.download = filename;
              a.click();
            },
          }
        );
      })) ||
    undefined;

  const label = useLabel(props.type);
  const dataCyProps = getDataCyProps(props.type);

  return (
    <ItemRow
      label={label}
      item={props.item}
      onDownloadReport={onDownload}
      tableRowProps={{ ...dataCyProps } as any}
    />
  );
};

function useLabel(type: ProportionalUsageType) {
  const { t } = useTranslate();
  if (type === 'SEATS') {
    return t('invoice_usage_dialog_table_seats_item');
  }
  if (type === 'TRANSLATIONS') {
    return t('invoice_usage_dialog_table_translations_item');
  }
  if (type === 'KEYS') {
    return t('invoice_usage_dialog_table_keys_item');
  }
  return '';
}

function getDataCyProps(type: ProportionalUsageType) {
  if (type === 'SEATS') {
    return { 'data-cy': 'billing-usage-table-seats' };
  }
  if (type === 'TRANSLATIONS') {
    return { 'data-cy': 'billing-usage-table-translations' };
  }
  if (type === 'KEYS') {
    return { 'data-cy': 'billing-usage-table-keys' };
  }
  return {};
}
