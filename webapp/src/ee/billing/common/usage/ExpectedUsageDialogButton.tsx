import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { DialogContent, DialogTitle, IconButton, Tooltip } from '@mui/material';
import { PieChart01 } from '@untitled-ui/icons-react';
import Dialog from '@mui/material/Dialog';
import { UsageTable } from './UsageTable';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';

export const ExpectedUsageDialogButton: FC<{
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
        disableInteractive
      >
        <IconButton
          size="small"
          onClick={onOpen}
          data-cy="billing-expected-usage-open-button"
        >
          <PieChart01 width={18} height={18} />
        </IconButton>
      </Tooltip>
      <Dialog
        open={open}
        onClose={onClose}
        maxWidth="md"
        data-cy="expected-usage-dialog"
      >
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
