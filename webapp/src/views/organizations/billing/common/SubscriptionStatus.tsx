import { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { Box, styled, useTheme } from '@mui/material';

const StyledBadge = styled(Box)`
  display: inline-flex;
  border-radius: 10px;
  padding: 2px 8px;
  align-items: center;
  text-align: center;
  justify-content: center;
`;

export const SubscriptionStatus: FC<{
  status: components['schemas']['EeSubscriptionModel']['status'];
}> = ({ status }) => {
  const { t } = useTranslate();

  const theme = useTheme();

  const statusMap: Record<
    typeof status,
    { label: string; color: 'success' | 'error' }
  > = {
    ACTIVE: { label: t('ee_license_status_label_active'), color: 'success' },
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
  };

  const paletteColor = theme.palette[statusMap[status].color];
  const backgroundColor = paletteColor.main;
  const contrastColor = paletteColor.contrastText;
  const label = statusMap[status].label;

  return (
    <StyledBadge
      sx={{
        backgroundColor: backgroundColor,
        color: contrastColor,
      }}
    >
      {label}
    </StyledBadge>
  );
};
