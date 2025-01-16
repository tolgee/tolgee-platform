import React, { FC, useMemo } from 'react';
import { useTrialInfo } from './announcements/useTrialInfo';
import { Box, Chip, Tooltip } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';

export const TrialChip: FC = () => {
  const {
    shouldShowChip,
    subscriptionsLink,
    daysLeft,
    isCurrentSubscriptionPage,
  } = useTrialInfo();

  const { t } = useTranslate();

  const Wrapper = useMemo(() => {
    return function Wrapper({ children }) {
      if (isCurrentSubscriptionPage) {
        return children;
      }

      return (
        <Tooltip title={t('topbar-trial-chip-tooltip', { daysLeft })}>
          {children}
        </Tooltip>
      );
    };
  }, [isCurrentSubscriptionPage]);

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
      <Wrapper>
        <Chip
          data-cy="topbar-trial-chip"
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
      </Wrapper>
    </Box>
  );
};
