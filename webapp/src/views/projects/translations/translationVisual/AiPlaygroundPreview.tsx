import {
  Box,
  styled,
  SxProps,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { Stars } from 'tg.component/CustomIcons';
import { components } from 'tg.service/apiSchema.generated';
import { AiReviewIssueList } from 'tg.ee';
import { TranslationVisual } from './TranslationVisual';

type AiReviewIssueModel = components['schemas']['AiReviewIssueModel'];

const StyledAiPreview = styled(Box)`
  display: grid;
  background: ${({ theme }) => theme.palette.tokens.secondary._states.selected};
  grid-template-columns: auto 1fr;
  gap: 8px;
  padding: 8px;
  border-radius: 8px;
`;

const StyledContent = styled('div')`
  display: grid;
  padding: 2px 0px;
`;

type Props = {
  translation: string | undefined;
  tooltip?: React.ReactNode;
  isPlural: boolean;
  locale: string;
  score?: number | null;
  issues?: AiReviewIssueModel[] | null;
  sx?: SxProps;
};

export const AiPlaygroundPreview = ({
  translation,
  tooltip,
  isPlural,
  locale,
  score,
  issues,
  sx,
}: Props) => {
  const theme = useTheme();
  const { t } = useTranslate();
  const isReview = score != null;
  const content = (
    <StyledAiPreview {...{ sx }} data-cy="ai-playground-preview">
      <Stars width={24} height={24} color={theme.palette.secondary.main} />
      <StyledContent>
        {isReview ? (
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
        ) : (
          <TranslationVisual
            text={translation}
            isPlural={isPlural}
            locale={locale}
            maxLines={3}
            extraPadding={false}
          />
        )}
      </StyledContent>
    </StyledAiPreview>
  );
  return tooltip ? <Tooltip title={tooltip}>{content}</Tooltip> : content;
};
