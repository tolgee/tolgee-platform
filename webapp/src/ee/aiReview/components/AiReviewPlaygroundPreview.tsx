import { Box, styled, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { Stars } from 'tg.component/CustomIcons';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { AiReviewPlaygroundPreviewProps } from 'eeSetup/EeModuleType';
import { AiReviewIssuesList } from 'tg.ee.module/aiReview/components/AiReviewIssuesList';

const StyledPreview = styled(Box)`
  display: grid;
  background: ${({ theme }) => theme.palette.tokens.secondary._states.selected};
  grid-template-columns: auto 1fr;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(1)};
  border-radius: ${({ theme }) => theme.shape.borderRadius * 2}px;
`;

const StyledContent = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: 2px 0px;
  min-width: 0;
`;

const StyledScore = styled('div')`
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
`;

export const AiReviewPlaygroundPreview = ({
  data,
}: AiReviewPlaygroundPreviewProps) => {
  const theme = useTheme();
  const { t } = useTranslate();

  return (
    <StyledPreview data-cy="ai-review-playground-preview">
      <Stars width={24} height={24} color={theme.palette.secondary.main} />
      <StyledContent>
        {data.reviewError ? (
          <Box
            sx={{ fontSize: 14, color: 'error.main' }}
            data-cy="ai-review-playground-preview-error"
          >
            <TranslatedError code={data.reviewError} />
          </Box>
        ) : (
          <>
            <StyledScore
              data-cy="ai-review-playground-preview-score"
              data-cy-score={data.reviewScore ?? 0}
            >
              {t('ai_review_playground_score', 'Score {score} / 100', {
                score: data.reviewScore ?? 0,
              })}
            </StyledScore>
            <AiReviewIssuesList issues={data.reviewIssues ?? []} />
          </>
        )}
      </StyledContent>
    </StyledPreview>
  );
};
