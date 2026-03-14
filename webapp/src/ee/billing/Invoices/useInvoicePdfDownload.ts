import { useConfig } from 'tg.globalContext/helpers';
import { components } from 'tg.service/billingApiSchema.generated';

type InvoiceModel = components['schemas']['InvoiceModel'];

export function useInvoicePdfDownload(invoice: InvoiceModel) {
  const config = useConfig();

  const onSuccess = async (response: unknown) => {
    const res = response as Response;
    const data = await res.blob();
    const url = URL.createObjectURL(data);
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
  };

  return { onSuccess };
}
