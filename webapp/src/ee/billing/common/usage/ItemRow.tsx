import { components } from 'tg.service/billingApiSchema.generated';
import { useMoneyFormatter, useNumberFormatter } from 'tg.hooks/useLocale';
import { IconButton, TableCell, TableRow, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Download02 } from '@untitled-ui/icons-react';

export const ItemRow = (props: {
  item:
    | components['schemas']['AverageProportionalUsageItemModel']
    | components['schemas']['SumUsageItemModel'];
  label: string;
  onDownloadReport?: () => void;
}) => {
  const formatMoney = useMoneyFormatter();
  const formatNumber = useNumberFormatter();

  const { t } = useTranslate();

  return (
    <TableRow>
      <TableCell>
        {props.label}{' '}
        {props.onDownloadReport && (
          <Tooltip title={t('invoice_usage_download_button')}>
            <IconButton size="small" onClick={props.onDownloadReport}>
              <Download02 />
            </IconButton>
          </Tooltip>
        )}
      </TableCell>
      <TableCell align="right">
        {formatNumber(props.item.usedQuantityOverPlan, {
          maximumFractionDigits: 0,
        })}
      </TableCell>
      <TableCell align="right">{formatMoney(props.item.total)}</TableCell>
    </TableRow>
  );
};
