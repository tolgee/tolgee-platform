import { MenuItem, styled, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { Select } from 'tg.component/common/Select';
import { AiReviewRuleMode } from 'tg.ee.module/aiReview/utils/types';

const StyledSection = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledHint = styled(Typography)`
  font-size: 13px;
`;

type Props = {
  value: AiReviewRuleMode;
  onChange: (value: AiReviewRuleMode) => void;
};

export const AiReviewModeField = ({ value, onChange }: Props) => {
  const { t } = useTranslate();

  return (
    <StyledSection>
      <Select
        label={t('ai_review_rule_mode_label', 'Mode')}
        value={value}
        onChange={(e) => onChange(e.target.value as AiReviewRuleMode)}
        minHeight={false}
        data-cy="ai-review-rule-mode-select"
      >
        <MenuItem
          value="DRY_RUN"
          data-cy="ai-review-rule-mode-option"
          data-cy-mode="DRY_RUN"
        >
          {t('ai_review_rule_mode_dry_run', 'Dry run')}
        </MenuItem>
        <MenuItem
          value="ACTIVE"
          data-cy="ai-review-rule-mode-option"
          data-cy-mode="ACTIVE"
        >
          {t('ai_review_rule_mode_active', 'Active')}
        </MenuItem>
      </Select>
      <StyledHint color="text.secondary">
        {value === 'DRY_RUN'
          ? t(
              'ai_review_rule_mode_dry_run_hint',
              'Scores translations without changing their state.'
            )
          : t(
              'ai_review_rule_mode_active_hint',
              'Marks translations at or above the threshold as reviewed.'
            )}
      </StyledHint>
    </StyledSection>
  );
};
