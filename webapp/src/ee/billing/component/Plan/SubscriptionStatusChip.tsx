import { Chip, ChipProps } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/billingApiSchema.generated';

type Status =
  components['schemas']['AdministrationBasicSubscriptionModel']['status'];

export const SubscriptionStatusChip = ({ status }: { status: Status }) => {
  const { t } = useTranslate();

  const STATUS_CONFIG: Record<
    Status,
    {
      color: ChipProps['color'];
      label: string;
    }
  > = {
    ACTIVE: { color: 'success', label: t('subscription_status_active') },
    TRIALING: { color: 'info', label: t('subscription_status_trialing') },
    CANCELED: { color: 'default', label: t('subscription_status_canceled') },
    PAST_DUE: { color: 'warning', label: t('subscription_status_past_due') },
    UNPAID: { color: 'error', label: t('subscription_status_unpaid') },
    ERROR: { color: 'error', label: t('subscription_status_error') },
    KEY_USED_BY_ANOTHER_INSTANCE: {
      color: 'default',
      label: t('subscription_status_key_used_by_another_instance'),
    },
    UNKNOWN: { color: 'default', label: t('subscription_status_unknown') },
  };

  const config = STATUS_CONFIG[status];

  if (!config) return null;

  return (
    <Chip
      size="small"
      color={config.color}
      label={config.label}
      data-cy={`subscription-status-${status.toLowerCase()}`}
    />
  );
};
