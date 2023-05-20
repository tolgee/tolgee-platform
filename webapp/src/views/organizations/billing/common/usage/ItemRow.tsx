import { components } from 'tg.service/billingApiSchema.generated';
import { useMoneyFormatter } from 'tg.hooks/useLocale';
import { IconButton, TableCell, TableRow, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Download } from '@mui/icons-material';

export const ItemRow = (props: {
  item:
    | components['schemas']['AverageProportionalUsageItemModel']
    | components['schemas']['SumUsageItemModel'];
  label: string;
  onDownloadReport?: () => void;
}) => {
  const formatMoney = useMoneyFormatter();

  const { t } = useTranslate();

  return (
    <TableRow>
      <TableCell>
        {props.label}{' '}
        {props.onDownloadReport && (
          <Tooltip title={t('invoice_usage_download_button')}>
            <IconButton size="small" onClick={props.onDownloadReport}>
              <Download />
            </IconButton>
          </Tooltip>
        )}
      </TableCell>
      <TableCell align="right">{props.item.usedQuantity}</TableCell>
      <TableCell align="right">{props.item.usedQuantityOverPlan}</TableCell>
      <TableCell align="right">{formatMoney(props.item.total)}</TableCell>
    </TableRow>
  );
};
