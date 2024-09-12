import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { TableCell, TableHead, TableRow } from '@mui/material';

export const UsageTableHead: FC = () => {
  const { t } = useTranslate();

  return (
    <TableHead>
      <TableRow>
        <TableCell>{t('invoice_usage_dialog_table_item')}</TableCell>
        <TableCell align="right">
          {t('invoice_usage_dialog_table_quantity_over_plan')}
        </TableCell>
        <TableCell align="right">
          {t('invoice_usage_dialog_table_subtotal')}
        </TableCell>
      </TableRow>
    </TableHead>
  );
};
