import { FC } from 'react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { components } from 'tg.service/billingApiSchema.generated';

type PdfDownloadButtonProps = {
  invoice: components['schemas']['InvoiceModel'];
  onDownload: () => void;
  isLoading: boolean;
};

export const PdfDownloadButton: FC<PdfDownloadButtonProps> = ({
  invoice,
  onDownload,
  isLoading,
}) => {
  return (
    <LoadingButton
      disabled={!invoice.pdfReady}
      loading={isLoading}
      onClick={onDownload}
      size="small"
    >
      PDF
    </LoadingButton>
  );
};
