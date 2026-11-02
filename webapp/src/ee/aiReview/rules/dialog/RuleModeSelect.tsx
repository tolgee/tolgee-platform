import {
  FormControl,
  FormHelperText,
  InputLabel,
  MenuItem,
  Select,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

type Props = {
  disabled: boolean;
};

export const RuleModeSelect = ({ disabled }: Props) => {
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();

  return (
    <FormControl size="small" disabled={disabled}>
      <InputLabel>{t('ai_review_rule_form_mode_label', 'Mode')}</InputLabel>
      <Select
        label={t('ai_review_rule_form_mode_label', 'Mode')}
        value={values.mode}
        onChange={(e) => setFieldValue('mode', e.target.value)}
        SelectDisplayProps={{ 'data-cy': 'ai-review-rule-form-mode' } as any}
      >
        <MenuItem value="DRY_RUN" data-cy="ai-review-rule-form-mode-dry-run">
          {t('ai_review_rule_form_mode_dry_run', 'Dry run')}
        </MenuItem>
        <MenuItem value="ACTIVE" data-cy="ai-review-rule-form-mode-active">
          {t('ai_review_rule_form_mode_active', 'Active')}
        </MenuItem>
      </Select>
      <FormHelperText>
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
    </FormControl>
  );
};
