import { Alert, FormControlLabel } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { Checkbox } from 'tg.component/common/form/fields/Checkbox';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

export const RuleNeverCheckbox = () => {
  const { t } = useTranslate();
  const { values } = useFormikContext<RuleFormValues>();

  return (
    <FormControlLabel
      sx={{ marginRight: 0, flexShrink: 0 }}
      control={
        <Checkbox
          name="never"
          size="small"
          checked={values.never}
          data-cy="ai-review-rule-form-never"
        />
      }
      label={t('ai_review_rule_form_never_use_label', 'Never use AI review')}
      slotProps={{ typography: { variant: 'body2' } }}
    />
  );
};

export const RuleNeverInfo = () => {
  const { t } = useTranslate();
  const { values } = useFormikContext<RuleFormValues>();

  if (!values.never) {
    return null;
  }

  return (
    <Alert severity="info" data-cy="ai-review-rule-form-never-info">
      {t(
        'ai_review_rule_form_never_use_info',
        'AI will never score or review these keys and languages, even if another rule matches them.'
      )}
    </Alert>
  );
};
