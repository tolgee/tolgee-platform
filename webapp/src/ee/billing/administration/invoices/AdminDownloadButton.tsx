import { FC } from 'react';

import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useInvoicePdfDownload } from '../../Invoices/useInvoicePdfDownload';
import { PdfDownloadButton } from '../../Invoices/PdfDownloadButton';

type AdminDownloadButtonProps = {
  invoice: components['schemas']['InvoiceModel'];
};

export const AdminDownloadButton: FC<AdminDownloadButtonProps> = ({
  invoice,
}) => {
  const { onSuccess } = useInvoicePdfDownload(invoice);

  const pdfMutation = useBillingApiMutation({
    url: '/v2/administration/billing/invoices/{invoiceId}/pdf',
    method: 'get',
    fetchOptions: { rawResponse: true },
  });

  const onDownload = () => {
    pdfMutation.mutate({ path: { invoiceId: invoice.id } }, { onSuccess });
  };

  return (
    <PdfDownloadButton
      invoice={invoice}
      onDownload={onDownload}
      isLoading={pdfMutation.isLoading}
    />
  );
};
