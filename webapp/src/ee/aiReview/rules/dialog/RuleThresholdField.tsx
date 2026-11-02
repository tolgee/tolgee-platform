import { Box, Checkbox, FormControlLabel } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { TextField } from 'tg.component/common/form/fields/TextField';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

export const RuleThresholdField = () => {
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();

  return (
    <Box display="grid" gap={0.5}>
      <TextField
        name="threshold"
        type="number"
        size="small"
        minHeight={false}
        disabled={values.never}
        label={t('ai_review_rule_form_threshold_label', 'Threshold')}
        helperText={t(
          'ai_review_rule_form_threshold_hint',
          'Translations scoring {threshold} or more are marked as reviewed automatically.',
          { threshold: String(values.threshold || 85) }
        )}
        data-cy="ai-review-rule-form-threshold"
        FormHelperTextProps={
          { 'data-cy': 'ai-review-rule-form-threshold-error' } as any
        }
      />
      <FormControlLabel
        control={
          <Checkbox
            checked={values.never}
            onChange={(_, checked) => setFieldValue('never', checked)}
            data-cy="ai-review-rule-form-never"
          />
        }
        label={t('ai_review_rule_form_never_label', 'Never auto-review')}
      />
    </Box>
  );
};
