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
  const isDryRun = rule.mode === 'DRY_RUN';
  const counts = rule.counts;

  if (!counts) {
    return null;
  }

  return (
    <>
      {isDryRun ? (
        <StyledInfoLine data-cy="ai-review-rule-counts-dry-run">
          {t(
            'ai_review_rule_counts_dry_run',
            '{scored, plural, one {# scored} other {# scored}} · {wouldBeAutoReviewed, plural, one {# would be auto-reviewed} other {# would be auto-reviewed}}',
            {
              scored: counts.scored,
              wouldBeAutoReviewed: counts.wouldBeAutoReviewed,
            }
          )}
        </StyledInfoLine>
      ) : (
        <StyledInfoLine
          data-cy="ai-review-rule-counts-active"
          data-cy-failed={String(counts.failed)}
        >
          <span>
            {t(
              'ai_review_rule_counts_active',
              '{autoReviewed, plural, one {# auto-reviewed} other {# auto-reviewed}} · {rejected, plural, one {# rejected} other {# rejected}} · {failed, plural, one {# failed} other {# failed}}',
              {
                autoReviewed: counts.autoReviewed,
                rejected: counts.rejected,
                failed: counts.failed,
              }
            )}
          </span>
          {canRun && counts.failed > 0 && (
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
        </StyledInfoLine>
      )}
      {counts.backlog > 0 && (
        <StyledInfoLine data-cy="ai-review-rule-backlog">
          <span>
            {isDryRun
              ? t(
                  'ai_review_rule_backlog_dry_run',
                  '{count, plural, one {# existing translation not scored} other {# existing translations not scored}}',
                  { count: counts.backlog }
                )
              : t(
                  'ai_review_rule_backlog_active',
                  '{count, plural, one {# existing translation not reviewed} other {# existing translations not reviewed}}',
                  { count: counts.backlog }
                )}
          </span>
          {canRun && (
            <>
              <Dot />
              <MuiLink
                component="button"
                onClick={() => onRun(runVariantForMode(rule.mode))}
                data-cy="ai-review-rule-review-now"
              >
                {isDryRun
                  ? t('ai_review_rule_score_now', 'Score now')
                  : t('ai_review_rule_review_now', 'Review now')}
              </MuiLink>
            </>
          )}
        </StyledInfoLine>
      )}
    </>
  );
};
