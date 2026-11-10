import { FormControlLabel } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { LabelHint } from 'tg.component/common/LabelHint';
import { Checkbox } from 'tg.component/common/form/fields/Checkbox';
import { useProject } from 'tg.hooks/useProject';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

export const RuleQaCheckbox = () => {
  const { t } = useTranslate();
  const project = useProject();
  const { values } = useFormikContext<RuleFormValues>();

  if (!project.useQaChecks) {
    return null;
  }

  return (
    <FormControlLabel
      // Set apart from the mode radios so it doesn't read as a third mode.
      sx={{ justifySelf: 'start', mt: 1.5 }}
      control={
        <Checkbox
          name="skipOpenQaIssues"
          size="small"
          checked={values.skipOpenQaIssues}
          data-cy="ai-review-rule-form-skip-qa"
        />
      }
      label={
        <LabelHint
          title={t(
            'ai_review_rule_form_skip_open_qa_issues_hint',
            "AI won't score or review translations until the issues are resolved."
          )}
        >
          {t(
            'ai_review_rule_form_skip_open_qa_issues_label',
            'Skip translations with open QA issues'
          )}
        </LabelHint>
      }
    />
  );
};
