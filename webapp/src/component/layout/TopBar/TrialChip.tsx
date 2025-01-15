import React, { FC } from 'react';
import { useTrialInfo } from './announcements/useTrialInfo';
import { Box, Chip, Tooltip } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';

export const TrialChip: FC = () => {
  const { shouldShowChip, subscriptionsLink, daysLeft } = useTrialInfo();

  const { t } = useTranslate();

  if (!shouldShowChip) {
    return null;
  }

  return (
    <Box
      sx={{
        display: 'flex',
        ml: '12px',
        justifyContent: 'center',
        alignItems: 'center',
      }}
    >
      <Tooltip title={t('topbar-trial-chip-tooltip', { daysLeft })}>
        <Chip
          sx={(theme) => ({
            flexGrow: 0,
            textTransform: 'uppercase',
            fontSize: 13,
            letterSpacing: '0.46px',
            fontWeight: 500,
            color: theme.palette.text.primary,
            backgroundColor: theme.palette.tokens.border.soft,
          })}
          size="small"
          label={<T keyName="topbar-trial-chip" />}
          component={Link}
          to={subscriptionsLink}
        />
      </Tooltip>
    </Box>
  );
};
