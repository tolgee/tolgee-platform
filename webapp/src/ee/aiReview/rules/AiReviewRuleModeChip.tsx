import { Chip, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { CheckCircleBroken } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];

type Props = {
  rule: AiReviewRuleModel;
};

export const AiReviewRuleModeChip = ({ rule }: Props) => {
  const { t } = useTranslate();
  const isScoreOnly = rule.mode === 'SCORE_ONLY';

  function modeLabel() {
    if (rule.never) {
      return t('ai_review_rule_mode_no_review', 'No AI review');
    }
    return isScoreOnly
      ? t('ai_review_rule_mode_score_only', 'AI score only')
      : t(
          'ai_review_rule_mode_auto_mark_threshold',
          'Auto-mark as reviewed · {threshold}+',
          { threshold: rule.threshold }
        );
  }

  function modeHint() {
    if (rule.never) {
      return t(
        'ai_review_rule_mode_no_review_hint',
        'AI never scores or reviews these keys and languages, even if another rule matches them.'
      );
    }
    return isScoreOnly
      ? t(
          'ai_review_rule_mode_score_only_hint',
          'AI scores translations and lists issues. Nothing else changes.'
        )
      : t(
          'ai_review_rule_mode_auto_mark_hint',
          'Translations scoring {threshold} or more are marked as reviewed. The rest wait for your team.',
          { threshold: rule.threshold }
        );
  }

  return (
    <Tooltip title={modeHint()} disableInteractive>
      <Chip
        size="small"
        data-cy="ai-review-rule-mode"
        data-cy-mode={rule.mode}
        label={modeLabel()}
        icon={
          !rule.never && !isScoreOnly ? (
            <CheckCircleBroken width={16} height={16} />
          ) : undefined
        }
        sx={
          rule.never
            ? undefined
            : (theme) => ({
                '& .MuiChip-icon': { color: 'inherit' },
                color: isScoreOnly
                  ? theme.palette.tokens.secondary.contrast
                  : theme.palette.tokens.success.contrast,
                backgroundColor: isScoreOnly
                  ? theme.palette.tokens.secondary.main
                  : theme.palette.tokens.success.main,
              })
        }
      />
    </Tooltip>
  );
};
