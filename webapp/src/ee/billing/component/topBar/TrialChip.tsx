import React, { FC, useState } from 'react';
import { useTrialInfo } from 'tg.component/layout/TopBar/announcements/useTrialInfo';
import { Box, Chip } from '@mui/material';
import { T } from '@tolgee/react';
import { TrialChipTooltip } from './TrialChipTooltip';

export const TrialChip: FC = () => {
  const { shouldShowChip } = useTrialInfo();

  const [tooltipOpen, setTooltipOpen] = useState(false);

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
      <TrialChipTooltip
        open={tooltipOpen}
        onOpen={() => setTooltipOpen(true)}
        onClose={() => setTooltipOpen(false)}
      >
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
          onClick={() => setTooltipOpen(true)}
        />
      </TrialChipTooltip>
    </Box>
  );
};
