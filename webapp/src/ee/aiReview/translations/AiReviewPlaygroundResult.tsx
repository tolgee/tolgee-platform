import { Box, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { AiReviewPlaygroundResultProps } from 'eeSetup/EeModuleType';
import { AiReviewIssueList } from 'tg.ee.module/aiReview/translations/AiReviewIssueList';

export const AiReviewPlaygroundResult = ({
  score,
  issues,
}: AiReviewPlaygroundResultProps) => {
  const { t } = useTranslate();

  return (
    <Box display="grid" gap={1}>
      <Typography
        fontWeight={500}
        data-cy="ai-playground-preview-score"
        data-cy-score={String(score)}
      >
        {t('ai_playground_preview_score', 'Score {score} / 100', {
          score,
        })}
      </Typography>
      {issues?.length ? (
        <AiReviewIssueList issues={issues} />
      ) : (
        <Typography
          variant="body2"
          color="textSecondary"
          data-cy="ai-playground-preview-no-issues"
        >
          {t('ai_playground_preview_no_issues', 'No issues found')}
        </Typography>
      )}
    </Box>
  );
};
