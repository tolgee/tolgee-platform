import React from 'react';
import { Box, ButtonBase, Link, Tooltip, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  AlertTriangle,
  CheckCircleBroken,
  Hourglass03,
} from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import {
  AiReviewRunVariant,
  runVariantForMode,
} from 'tg.ee.module/aiReview/runs/AiReviewRunDialog';

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];

// Stats as icon + number (details in the tooltip), then "Retry failed" a bit apart.
const StyledCounts = styled(Box)`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  column-gap: ${({ theme }) => theme.spacing(3)};
  row-gap: ${({ theme }) => theme.spacing(0.5)};
  flex-shrink: 0;
  white-space: nowrap;
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: ${({ theme }) => theme.typography.body2.fontSize}px;

  & svg {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
  }
`;

const StyledStats = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1.5)};
`;

const StyledStat = styled('span')`
  display: inline-flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
  cursor: default;
  font-variant-numeric: tabular-nums;

  &.error {
    color: ${({ theme }) => theme.palette.error.main};
  }
`;

// The waiting count is also the button that reviews them, grey like the other stats, pink on hover.
const StyledStatButton = styled(ButtonBase)`
  display: inline-flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
  font: inherit;
  color: inherit;
  font-variant-numeric: tabular-nums;
  border-radius: 4px;

  &:hover,
  &.Mui-focusVisible {
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

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

  const waitingTip = isScoreOnly
    ? t(
        'ai_review_rule_waiting_score_tooltip',
        '{count, plural, other {# waiting for a score}}',
        { count: counts.backlog }
      )
    : t(
        'ai_review_rule_waiting_review_tooltip',
        '{count, plural, other {# waiting for AI review}}',
        { count: counts.backlog }
      );
  const waitingAction = isScoreOnly
    ? t(
        'ai_review_rule_score_waiting',
        '{count, plural, other {Score # waiting}}',
        { count: counts.backlog }
      )
    : t(
        'ai_review_rule_review_waiting',
        '{count, plural, other {Review # waiting}}',
        { count: counts.backlog }
      );
  const markedText = t(
    'ai_review_rule_counts_marked',
    '{count, plural, other {# marked as reviewed}}',
    { count: counts.autoReviewed }
  );
  const failedText = t(
    'ai_review_rule_counts_failed_tooltip',
    '{count, plural, one {# failed review} other {# failed reviews}}',
    { count: counts.failed }
  );
  const waitingContent = (
    <>
      <Hourglass03 />
      {counts.backlog}
    </>
  );

  return (
    <StyledCounts
      data-cy="ai-review-rule-counts"
      data-cy-mode={isScoreOnly ? 'SCORE_ONLY' : 'ACTIVE'}
      data-cy-failed={String(counts.failed)}
      onClick={(e: React.MouseEvent) => e.stopPropagation()}
    >
      <StyledStats>
        {isScoreOnly ? (
          // No icon reads as "scored" well, so it stays a short text.
          <StyledStat data-cy="ai-review-rule-scored">
            {t(
              'ai_review_rule_counts_scored',
              '{count, plural, other {# scored}}',
              { count: counts.scored }
            )}
          </StyledStat>
        ) : (
          <Tooltip title={markedText} disableInteractive>
            {/* Icon + number on screen; screen readers read the whole sentence. */}
            <StyledStat
              role="img"
              aria-label={markedText}
              data-cy="ai-review-rule-marked"
            >
              <CheckCircleBroken />
              {counts.autoReviewed}
            </StyledStat>
          </Tooltip>
        )}
        {counts.backlog > 0 &&
          (canRun ? (
            <Tooltip
              title={`${waitingTip} · ${waitingAction}`}
              disableInteractive
            >
              <StyledStatButton
                onClick={() => onRun(runVariantForMode(rule.mode))}
                aria-label={`${waitingTip} · ${waitingAction}`}
                data-cy="ai-review-rule-review-now"
              >
                {waitingContent}
              </StyledStatButton>
            </Tooltip>
          ) : (
            <Tooltip title={waitingTip} disableInteractive>
              <StyledStat
                role="img"
                aria-label={waitingTip}
                data-cy="ai-review-rule-waiting"
              >
                {waitingContent}
              </StyledStat>
            </Tooltip>
          ))}
        {/* Score only rules never showed failures; that stays. */}
        {!isScoreOnly && counts.failed > 0 && (
          <Tooltip title={failedText} disableInteractive>
            <StyledStat
              className="error"
              role="img"
              aria-label={failedText}
              data-cy="ai-review-rule-failed"
            >
              <AlertTriangle />
              {counts.failed}
            </StyledStat>
          </Tooltip>
        )}
      </StyledStats>
      {canRun && !isScoreOnly && counts.failed > 0 && (
        <Link
          component="button"
          color="inherit"
          underline="hover"
          onClick={() => onRun('retry')}
          sx={(theme) => ({
            '&:hover': { color: theme.palette.primary.main },
          })}
          data-cy="ai-review-rule-retry-failed"
        >
          {t('ai_review_rule_retry_failed_short', 'Retry failed')}
        </Link>
      )}
    </StyledCounts>
  );
};
