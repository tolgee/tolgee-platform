import { Box, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { AiReviewPlaygroundResultProps } from 'eeSetup/EeModuleType';
import { AiReviewIssues } from 'tg.ee.module/aiReview/translations/AiReviewIssues';
import { AiReviewScoreTitle } from 'tg.ee.module/aiReview/translations/AiReviewScoreTitle';

// The same parts as the AI review card: the score title, then the issues.
export const AiReviewPlaygroundResult = ({
  score,
  issues,
}: AiReviewPlaygroundResultProps) => {
  const { t } = useTranslate();

  return (
    <Box display="grid" gap={1.5}>
      <AiReviewScoreTitle
        title={t('ai_review_popover_title_score', 'AI review score')}
        score={score}
        scoreProps={{
          'data-cy': 'ai-playground-preview-score',
          'data-cy-score': String(score),
        }}
      />
      {issues?.length ? (
        <AiReviewIssues issues={issues} />
      ) : (
        <Typography
          color="text.secondary"
          sx={{ fontSize: 13, lineHeight: '18px', mt: -1 }}
          data-cy="ai-playground-preview-no-issues"
        >
          {t('ai_playground_preview_no_issues', 'No issues found')}
        </Typography>
      )}
    </Box>
  );
};
