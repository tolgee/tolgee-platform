import { useTranslate } from '@tolgee/react';
import { styled, useTheme } from '@mui/material';
import { PlanSubtitle } from '../Plan/PlanStyles';
import { components } from 'tg.service/apiSchema.generated';

type EeSubscriptionModelStatus =
  components['schemas']['EeSubscriptionModel']['status'];

const StyledCustomLabel = styled('span')`
  font-size: 14px;
  font-weight: 400;
  text-transform: lowercase;
`;

type Props = {
  status: EeSubscriptionModelStatus;
  custom?: boolean;
};

export const ActiveSubscriptionBanner = ({ status, custom }: Props) => {
  const { t } = useTranslate();
  const theme = useTheme();

  const statusMap: Record<
    EeSubscriptionModelStatus,
    { label: string; color: 'secondary' | 'error' }
  > = {
    ACTIVE: { label: t('ee_license_status_label_active'), color: 'secondary' },
    // we don't support trials for EE yet, so let's use the same as for active (if it happens by any chance)
    TRIALING: {
      label: t('ee_license_status_label_active'),
      color: 'secondary',
    },
    CANCELED: {
      label: t('ee_license_status_label_canceled'),
      color: 'error',
    },
    PAST_DUE: {
      label: t('ee_license_status_label_past_due'),
      color: 'error',
    },
    ERROR: { label: t('ee_license_status_label_error'), color: 'error' },
    UNPAID: { label: t('ee_license_status_label_unpaid'), color: 'error' },
    KEY_USED_BY_ANOTHER_INSTANCE: {
      label: t('ee_license_status_label_key_used_by_another_instance'),
      color: 'error',
    },
    UNKNOWN: {
      label: t('ee_license_status_label_unknown'),
      color: 'error',
    },
  };

  const { label, color } = statusMap[status];
  const clrObject = theme.palette.tokens[color];

  return (
    <PlanSubtitle
      data-cy="billing-plan-subtitle"
      sx={{ background: clrObject._states.hover, color: clrObject.main }}
    >
      <span>{label}</span>
      {custom && (
        <>
          {' '}
          <StyledCustomLabel className="noBackground">
            {t('billing_subscription_custom')}
          </StyledCustomLabel>
        </>
      )}
    </PlanSubtitle>
  );
};
