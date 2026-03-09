import { FC } from 'react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { components } from 'tg.service/billingApiSchema.generated';
import { useConfig } from 'tg.globalContext/helpers';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';

type AdminDownloadButtonProps = {
  invoice: components['schemas']['InvoiceModel'];
};

export const AdminDownloadButton: FC<AdminDownloadButtonProps> = ({
  invoice,
}) => {
  const config = useConfig();

  const pdfMutation = useBillingApiMutation({
    url: '/v2/administration/billing/invoices/{invoiceId}/pdf',
    method: 'get',
    fetchOptions: {
      rawResponse: true,
    },
  });

  const onDownload = () => {
    pdfMutation.mutate(
      {
        path: {
          invoiceId: invoice.id,
        },
      },
      {
        async onSuccess(response) {
          const res = response as unknown as Response;
          const data = await res.blob();
          const url = URL.createObjectURL(data as unknown as Blob);
          try {
            const a = document.createElement('a');
            try {
              a.href = url;
              a.download = `${config.appName.toLowerCase()}-${invoice.number}.pdf`;
              a.click();
            } finally {
              a.remove();
            }
          } finally {
            setTimeout(() => URL.revokeObjectURL(url), 7000);
          }
        },
      }
    );
  };

  return (
    <LoadingButton
      disabled={!invoice.pdfReady}
      loading={pdfMutation.isLoading}
      onClick={onDownload}
      size="small"
    >
      PDF
    </LoadingButton>
  );
};
