import {
  Box,
  DialogContent,
  DialogTitle,
  IconButton,
  Table,
  TableBody,
  Tooltip,
} from '@mui/material';
import { DataUsage } from '@mui/icons-material';
import { FC, useState } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import Dialog from '@mui/material/Dialog';
import { useOrganization } from '../../useOrganization';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { PeriodRew } from '../common/usage/PeriodRew';
import { SubscriptionRow } from '../common/usage/SubscriptionRow';
import { UsageTableHead } from '../common/usage/UsageTableHead';
import { TotalRow } from '../common/usage/TotalRow';
import { TotalTable } from '../common/usage/TotalTable';

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
              aria-label={t('billing_invoices_show_usage_button')}
            >
              <Tooltip title={t('billing_invoices_show_usage_button')}>
                <DataUsage />
              </Tooltip>
            </IconButton>
          </Box>
          <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md">
            <DialogTitle>{t('invoice_usage_dialog_title')}</DialogTitle>
            <DialogContent>
              {usage.data ? (
                <>
                  <Table>
                    <UsageTableHead />
                    <TableBody>
                      <SubscriptionRow price={usage.data?.subscriptionPrice} />
                      {(usage.data?.periods || []).map((period) => (
                        <PeriodRew key={period.from} period={period} />
                      ))}
                      <TotalRow total={usage.data.total} />
                    </TableBody>
                  </Table>

                  <TotalTable
                    invoice={invoice}
                    totalWithoutVat={usage.data.total}
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
