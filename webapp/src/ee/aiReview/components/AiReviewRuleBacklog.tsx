import { useTranslate } from '@tolgee/react';

import { AiReviewRuleModel } from 'tg.ee.module/aiReview/utils/types';
import {
  StyledRuleDetails,
  StyledRuleLink,
} from 'tg.ee.module/aiReview/components/AiReviewRuleRowStyles';

type Props = {
  rule: AiReviewRuleModel;
  backlog: number;
  onRun: () => void;
};

export const AiReviewRuleBacklog = ({ rule, backlog, onRun }: Props) => {
  const { t } = useTranslate();
  const dryRun = rule.mode === 'DRY_RUN';

  return (
    <StyledRuleDetails data-cy="ai-review-rule-backlog" data-cy-count={backlog}>
      <span>
        {dryRun
          ? t(
              'ai_review_rule_backlog_dry_run',
              '{count, plural, one {# existing translation not scored} other {# existing translations not scored}}',
              { count: backlog }
            )
          : t(
              'ai_review_rule_backlog',
              '{count, plural, one {# existing translation not reviewed} other {# existing translations not reviewed}}',
              { count: backlog }
            )}
      </span>
      <span className="separator">·</span>
      <StyledRuleLink
        underline="none"
        onClick={(e) => {
          e.stopPropagation();
          onRun();
        }}
        data-cy="ai-review-rule-backlog-run"
      >
        {dryRun
          ? t('ai_review_rule_backlog_score_now', 'Score now')
          : t('ai_review_rule_backlog_review_now', 'Review now')}
      </StyledRuleLink>
    </StyledRuleDetails>
  );
};
