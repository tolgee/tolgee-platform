import { FC } from 'react';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { components } from 'tg.service/billingApiSchema.generated';
import { useConfig } from 'tg.globalContext/helpers';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';

type DownloadButtonProps = {
  invoice: components['schemas']['InvoiceModel'];
};

export const DownloadButton: FC<DownloadButtonProps> = (props) => {
  const organization = useOrganization();
  const config = useConfig();

  const pdfMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/invoices/{invoiceId}/pdf',
    method: 'get',
    fetchOptions: {
      rawResponse: true,
    },
  });

  const onDownload = () => {
    pdfMutation.mutate(
      {
        path: {
          organizationId: organization!.id,
          invoiceId: props.invoice.id,
        },
      },
      {
        async onSuccess(response) {
          const res = response as unknown as Response;
          const data = await res.blob();
          const url = URL.createObjectURL(data as any as Blob);
          try {
            const a = document.createElement('a');
            try {
              a.href = url;
              a.download = `${config.appName.toLowerCase()}-${
                props.invoice.number
              }.pdf`;

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
      disabled={!props.invoice.pdfReady}
      loading={pdfMutation.isLoading}
      onClick={onDownload}
      size="small"
    >
      PDF
    </LoadingButton>
  );
};
