import { Box, Checkbox, FormControlLabel, FormHelperText } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { TextField } from 'tg.component/common/form/fields/TextField';
import { FieldLabel } from 'tg.component/FormField';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

export const RuleThresholdField = () => {
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();

  return (
    <Box display="grid" gap={0.5}>
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
      <FormHelperText sx={{ marginTop: -1 }}>
        {t(
          'ai_review_rule_form_never_hint',
          'Never run any AI review rule for the matching keys.'
        )}
      </FormHelperText>
      <FieldLabel>
        {t('ai_review_rule_form_threshold_label', 'Threshold')}
      </FieldLabel>
      <TextField
        name="threshold"
        type="number"
        size="small"
        minHeight={false}
        disabled={values.never}
        helperText={t(
          'ai_review_rule_form_threshold_hint',
          'Translations scoring {threshold} or more are marked as reviewed automatically.',
          { threshold: String(values.threshold === '' ? 85 : values.threshold) }
        )}
        data-cy="ai-review-rule-form-threshold"
        FormHelperTextProps={
          { 'data-cy': 'ai-review-rule-form-threshold-error' } as any
        }
      />
    </Box>
  );
};
