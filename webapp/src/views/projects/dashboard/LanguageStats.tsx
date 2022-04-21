import React, { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { Box } from '@mui/material';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';

export const LanguageStats: FC<{
  languageStats: components['schemas']['LanguageStatsModel'][];
}> = ({ languageStats }) => (
  <>
    {languageStats.map((languageStats) => (
      <Box key={languageStats.languageId}>
        <CircledLanguageIcon
          size={20}
          flag={languageStats.languageFlagEmoji || ''}
        />
        <Box>Translated: {languageStats.translatedPercentage}</Box>
        <Box>Reviewed: {languageStats.reviewedPercentage}</Box>
      </Box>
    ))}
  </>
);
