import {
  Box,
  DialogContent,
  DialogTitle,
  IconButton,
  Tooltip,
} from '@mui/material';
import { DataUsage } from '@mui/icons-material';
import { FC, useState } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import Dialog from '@mui/material/Dialog';
import { useOrganization } from '../../useOrganization';

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
              aria-label={t('billing_invoices_show_usage_button')}
            >
              <Tooltip title={t('billing_invoices_show_usage_button')}>
                <DataUsage />
              </Tooltip>
            </IconButton>
          </Box>
          <Dialog open={open} onClose={() => setOpen(false)}>
            <DialogTitle>{t('invoice_usage_dialog_title')}</DialogTitle>
            <DialogContent></DialogContent>
          </Dialog>
        </>
      )}
    </>
  );
};
