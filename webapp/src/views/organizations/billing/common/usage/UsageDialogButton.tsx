import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { DialogContent, DialogTitle, IconButton, Tooltip } from '@mui/material';
import { DataUsage } from '@mui/icons-material';
import Dialog from '@mui/material/Dialog';
import { UsageTable } from './UsageTable';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';

export const UsageDialogButton: FC<{
  usageData?: components['schemas']['UsageModel'];
  loading: boolean;
  onOpen: () => void;
  onClose: () => void;
  open: boolean;
}> = ({ usageData, onOpen, onClose, loading, open }) => {
  const { t } = useTranslate();

  return (
    <>
      <Tooltip
        title={t('active-plan-estimated-costs-show-usage-button-tooltip')}
      >
        <IconButton
          size="small"
          onClick={onOpen}
          data-cy="billing-estimated-costs-open-button"
        >
          <DataUsage />
        </IconButton>
      </Tooltip>
      <Dialog open={open} onClose={onClose} maxWidth="md">
        <DialogTitle>{t('invoice_usage_dialog_title')}</DialogTitle>
        <DialogContent>
          {usageData ? (
            <>
              <UsageTable usageData={usageData} />
            </>
          ) : (
            <EmptyListMessage loading={loading} />
          )}
        </DialogContent>
      </Dialog>
    </>
  );
};
