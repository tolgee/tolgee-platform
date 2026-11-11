import { Link as MuiLink, Typography, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import {
  AiReviewRunVariant,
  runVariantForMode,
} from 'tg.ee.module/aiReview/runs/AiReviewRunDialog';

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];

export const StyledInfoLine = styled(Typography)`
  display: flex;
  flex-wrap: wrap;
  column-gap: ${({ theme }) => theme.spacing(0.5)};
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: ${({ theme }) => theme.typography.body2.fontSize}px;
`;

export const Dot = () => <span aria-hidden="true">·</span>;

type Props = {
  rule: AiReviewRuleModel;
  canRun: boolean;
  onRun: (variant: AiReviewRunVariant) => void;
};

export const AiReviewRuleCounts = ({ rule, canRun, onRun }: Props) => {
  const { t } = useTranslate();
  const isScoreOnly = rule.mode === 'SCORE_ONLY';
  const counts = rule.counts;

  if (!counts) {
    return null;
  }

  const parts: string[] = isScoreOnly
    ? [
        t('ai_review_rule_counts_scored', '{count, plural, other {# scored}}', {
          count: counts.scored,
        }),
      ]
    : [
        t(
          'ai_review_rule_counts_marked',
          '{count, plural, other {# marked as reviewed}}',
          { count: counts.autoReviewed }
        ),
        ...(counts.failed > 0
          ? [
              t(
                'ai_review_rule_counts_failed',
                '{count, plural, other {# failed}}',
                { count: counts.failed }
              ),
            ]
          : []),
      ];

  return (
    <StyledInfoLine
      data-cy="ai-review-rule-counts"
      data-cy-mode={isScoreOnly ? 'SCORE_ONLY' : 'ACTIVE'}
      data-cy-failed={String(counts.failed)}
      onClick={(e) => e.stopPropagation()}
    >
      {parts.map((part, index) => (
        <span key={index}>
          {index > 0 && <Dot />} {part}
        </span>
      ))}
      {canRun && !isScoreOnly && counts.failed > 0 && (
        <>
          <Dot />
          <MuiLink
            component="button"
            onClick={() => onRun('retry')}
            data-cy="ai-review-rule-retry-failed"
          >
            {t('ai_review_rule_retry_failed', 'Retry failed reviews')}
          </MuiLink>
        </>
      )}
      {canRun && counts.backlog > 0 && (
        <>
          <Dot />
          <MuiLink
            component="button"
            onClick={() => onRun(runVariantForMode(rule.mode))}
            data-cy="ai-review-rule-review-now"
          >
            {isScoreOnly
              ? t(
                  'ai_review_rule_score_waiting',
                  '{count, plural, other {Score # waiting}}',
                  { count: counts.backlog }
                )
              : t(
                  'ai_review_rule_review_waiting',
                  '{count, plural, other {Review # waiting}}',
                  { count: counts.backlog }
                )}
          </MuiLink>
        </>
      )}
    </StyledInfoLine>
  );
};
