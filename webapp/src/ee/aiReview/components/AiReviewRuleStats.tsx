import { useTranslate } from '@tolgee/react';

import { AiReviewRuleModel } from 'tg.ee.module/aiReview/utils/types';
import {
  StyledRuleDetails,
  StyledRuleLink,
} from 'tg.ee.module/aiReview/components/AiReviewRuleRowStyles';

type Props = {
  rule: AiReviewRuleModel;
  canEdit: boolean;
  onRetryFailed: () => void;
};

export const AiReviewRuleStats = ({ rule, canEdit, onRetryFailed }: Props) => {
  const { t } = useTranslate();

  return (
    <StyledRuleDetails
      data-cy="ai-review-rule-stats"
      data-cy-auto-reviewed={rule.stats.autoReviewed}
    >
      {rule.mode === 'DRY_RUN'
        ? t(
            'ai_review_rule_stats_dry_run',
            '{scored} scored · {wouldAutoReview} would be auto-reviewed',
            {
              scored: rule.stats.scored,
              wouldAutoReview: rule.stats.wouldAutoReview,
            }
          )
        : t(
            'ai_review_rule_stats_active',
            '{autoReviewed} auto-reviewed · {rejected} rejected',
            {
              autoReviewed: rule.stats.autoReviewed,
              rejected: rule.stats.rejected,
            }
          )}
      {rule.stats.failed > 0 && (
        <>
          <span className="separator">·</span>
          <span
            data-cy="ai-review-rule-stats-failed"
            data-cy-count={rule.stats.failed}
          >
            {t('ai_review_rule_stats_failed', '{failed} failed', {
              failed: rule.stats.failed,
            })}
          </span>
          {canEdit && (
            <>
              <span className="separator">·</span>
              <StyledRuleLink
                underline="none"
                onClick={(e) => {
                  e.stopPropagation();
                  onRetryFailed();
                }}
                data-cy="ai-review-rule-retry-failed-link"
              >
                {t('ai_review_rule_menu_retry_failed', 'Retry failed reviews')}
              </StyledRuleLink>
            </>
          )}
        </>
      )}
    </StyledRuleDetails>
  );
};
