import { FC } from 'react';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useInvoicePdfDownload } from './useInvoicePdfDownload';
import { PdfDownloadButton } from './PdfDownloadButton';

type DownloadButtonProps = {
  invoice: components['schemas']['InvoiceModel'];
};

export const DownloadButton: FC<DownloadButtonProps> = ({ invoice }) => {
  const organization = useOrganization();
  const { onSuccess } = useInvoicePdfDownload(invoice);

  const pdfMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/invoices/{invoiceId}/pdf',
    method: 'get',
    fetchOptions: { rawResponse: true },
  });

  const onDownload = () => {
    pdfMutation.mutate(
      { path: { organizationId: organization!.id, invoiceId: invoice.id } },
      { onSuccess }
    );
  };

  return (
    <PdfDownloadButton
      invoice={invoice}
      onDownload={onDownload}
      isLoading={pdfMutation.isLoading}
    />
  );
};
