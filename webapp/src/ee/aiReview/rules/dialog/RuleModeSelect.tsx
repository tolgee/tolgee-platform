import { Box, FormControlLabel, Radio, RadioGroup } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { FieldLabel } from 'tg.component/FormField';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

export const RuleModeSelect = () => {
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();

  return (
    <Box display="grid">
      <FieldLabel>
        {t('ai_review_rule_form_review_mode_label', 'Review mode')}
      </FieldLabel>
      <RadioGroup
        value={values.mode}
        onChange={(_, mode) => setFieldValue('mode', mode)}
        data-cy="ai-review-rule-form-mode"
      >
        <FormControlLabel
          value="SCORE_ONLY"
          sx={{ justifySelf: 'start' }}
          control={
            <Radio
              size="small"
              sx={{ py: 0.25 }}
              data-cy="ai-review-rule-form-mode-score-only"
            />
          }
          label={t('ai_review_rule_mode_score_only', 'AI score only')}
        />
        <FormControlLabel
          value="ACTIVE"
          sx={{ justifySelf: 'start' }}
          control={
            <Radio
              size="small"
              sx={{ py: 0.25 }}
              data-cy="ai-review-rule-form-mode-active"
            />
          }
          label={t(
            'ai_review_rule_form_score_and_mark_label',
            'Score and mark as reviewed'
          )}
        />
      </RadioGroup>
    </Box>
  );
};
