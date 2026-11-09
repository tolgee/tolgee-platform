import { Box, FormHelperText } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { FieldLabel } from 'tg.component/FormField';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';
import { SegmentedControl } from 'tg.component/common/SegmentedControl';

export const RuleModeSelect = () => {
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();

  return (
    <Box display="grid" gap={1}>
      <FieldLabel>{t('ai_review_rule_form_mode_label', 'Mode')}</FieldLabel>
      <SegmentedControl
        dataCy="ai-review-rule-form-mode"
        value={values.mode}
        onChange={(mode) => setFieldValue('mode', mode)}
        options={[
          {
            value: 'DRY_RUN',
            title: t('ai_review_rule_form_mode_dry_run', 'Dry run'),
            dataCy: 'ai-review-rule-form-mode-dry-run',
          },
          {
            value: 'ACTIVE',
            title: t('ai_review_rule_form_mode_active', 'Active'),
            dataCy: 'ai-review-rule-form-mode-active',
          },
        ]}
      />
      <FormHelperText sx={{ marginTop: -0.5 }}>
        {values.mode === 'DRY_RUN'
          ? t(
              'ai_review_rule_form_mode_dry_run_hint',
              'The AI only scores translations. Nothing is marked as reviewed.'
            )
          : t(
              'ai_review_rule_form_mode_active_hint',
              'Translations that pass are marked as reviewed.'
            )}
      </FormHelperText>
    </Box>
  );
};
