import { Checkbox, styled, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

const StyledCard = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(2)};
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 8px;
  padding: ${({ theme }) => theme.spacing(1.5, 2)};
`;

export const RuleNeverCard = () => {
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();

  return (
    <StyledCard>
      <div>
        <Typography>
          {t('ai_review_rule_form_never_label', 'Never auto-review')}
        </Typography>
        <Typography variant="body2" color="textSecondary">
          {t(
            'ai_review_rule_form_never_hint',
            'Never run any AI review rule for the matching keys.'
          )}
        </Typography>
      </div>
      <Checkbox
        checked={values.never}
        onChange={(_, checked) => setFieldValue('never', checked)}
        data-cy="ai-review-rule-form-never"
      />
    </StyledCard>
  );
};
