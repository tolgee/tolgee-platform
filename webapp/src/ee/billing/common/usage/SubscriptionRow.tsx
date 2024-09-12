import { components } from 'tg.service/billingApiSchema.generated';
import { useMoneyFormatter } from 'tg.hooks/useLocale';
import { useTranslate } from '@tolgee/react';
import { TableCell, TableRow } from '@mui/material';

export const SubscriptionRow = (props: {
  price: components['schemas']['UsageModel']['subscriptionPrice'];
}) => {
  const formatMoney = useMoneyFormatter();

  const { t } = useTranslate();

  return (
    <TableRow>
      <TableCell>{t('invoice_usage_dialog_table_subscription_item')}</TableCell>
      <TableCell align="right">
        {t('invoice_usage_dialog_table_no_value')}
      </TableCell>
      <TableCell align="right">{formatMoney(props.price!)}</TableCell>
    </TableRow>
  );
};
