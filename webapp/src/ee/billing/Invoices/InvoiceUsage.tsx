import {
  Box,
  DialogContent,
  DialogTitle,
  IconButton,
  Tooltip,
} from '@mui/material';
import { PieChart01 } from '@untitled-ui/icons-react';
import { FC, useState } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import Dialog from '@mui/material/Dialog';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { TotalTable } from '../common/usage/TotalTable';
import { UsageTable } from '../common/usage/UsageTable';

export const InvoiceUsage: FC<{
  invoice: components['schemas']['InvoiceModel'];
}> = ({ invoice }) => {
  const { t } = useTranslate();

  const [open, setOpen] = useState(false);

  const organization = useOrganization();

  const usage = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/invoices/{invoiceId}/usage',
    method: 'get',
    path: {
      organizationId: organization!.id,
      invoiceId: invoice.id,
    },
    options: {
      enabled: open,
    },
  });

  return (
    <>
      {invoice.hasUsage && (
        <>
          <Box>
            <IconButton
              size="small"
              onClick={() => setOpen(true)}
              data-cy="billing-invoice-usage-button"
              aria-label={t('billing_invoices_show_usage_button')}
            >
              <Tooltip title={t('billing_invoices_show_usage_button')}>
                <PieChart01 />
              </Tooltip>
            </IconButton>
          </Box>
          <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md">
            <DialogTitle>{t('invoice_usage_dialog_title')}</DialogTitle>
            <DialogContent>
              {usage.data ? (
                <>
                  <UsageTable
                    usageData={usage.data}
                    invoiceId={invoice.id}
                    invoiceNumber={invoice.number}
                  ></UsageTable>
                  <TotalTable
                    invoice={invoice}
                    totalWithoutVat={usage.data.total}
                    appliedStripeCredits={usage.data.appliedStripeCredits}
                  />
                </>
              ) : (
                <EmptyListMessage loading={usage.isLoading} />
              )}
            </DialogContent>
          </Dialog>
        </>
      )}
    </>
  );
};
