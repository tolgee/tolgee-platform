import { FC } from 'react';
import { useTranslate } from '@tolgee/react';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { components } from 'tg.service/billingApiSchema.generated';
import { useConfig } from 'tg.globalContext/helpers';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { container } from 'tsyringe';
import { MessageService } from 'tg.service/MessageService';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';

type DownloadButtonProps = {
  invoice: components['schemas']['InvoiceModel'];
};

const messaging = container.resolve(MessageService);

export const DownloadButton: FC<DownloadButtonProps> = (props) => {
  const organization = useOrganization();
  const config = useConfig();
  const t = useTranslate();

  const pdfMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/invoices/{invoiceId}/pdf',
    method: 'get',
    fetchOptions: {
      asBlob: true,
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
        onSuccess(blob) {
          const url = URL.createObjectURL(blob as any as Blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `${config.appName.toLowerCase()}-${
            props.invoice.number
          }.pdf`;

          a.click();
          a.remove();
          setTimeout(() => URL.revokeObjectURL(a.href), 7000);
        },
        onError(error) {
          parseErrorResponse(error).map((e) => messaging.error(t(e)));
        },
      }
    );
  };

  return (
    <LoadingButton
      disabled={!props.invoice.pdfReady}
      loading={pdfMutation.isLoading}
      onClick={onDownload}
    >
      PDF
    </LoadingButton>
  );
};
