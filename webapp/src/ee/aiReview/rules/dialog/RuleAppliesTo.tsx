import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';
import { RuleKeysFilter } from 'tg.ee.module/aiReview/rules/dialog/RuleKeysFilter';
import { RuleLanguagesField } from 'tg.ee.module/aiReview/rules/dialog/RuleLanguagesField';
import { ScopeAutocomplete } from 'tg.ee.module/aiReview/rules/dialog/ScopeAutocomplete';
import { useRuleScopeOptions } from 'tg.ee.module/aiReview/rules/dialog/useRuleScopeOptions';

const StyledTwoColumns = styled('div')`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacing(2)};
  align-items: start;

  ${({ theme }) => theme.breakpoints.down('sm')} {
    grid-template-columns: minmax(0, 1fr);
  }
`;

export const RuleAppliesTo = () => {
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();
  const { isBranchingEnabled, branchOptions } = useRuleScopeOptions();

  function setScope(
    field: 'branchIn' | 'branchNotIn',
    siblingField: 'branchIn' | 'branchNotIn'
  ) {
    return (next: string[]) => {
      setFieldValue(field, next);
      setFieldValue(
        siblingField,
        values[siblingField].filter((item) => !next.includes(item))
      );
    };
  }

  const branchesHint = t(
    'ai_review_rule_form_branches_hint',
    'Type a branch name or a pattern like feature/*'
  );

  return (
    <Box display="grid" gap={1.5}>
      <StyledTwoColumns>
        <RuleKeysFilter />
        <RuleLanguagesField />
      </StyledTwoColumns>
      {isBranchingEnabled && (
        <StyledTwoColumns>
          <ScopeAutocomplete
            label={t('ai_review_rule_form_branches_label', 'Branches')}
            value={values.branchIn}
            options={branchOptions.filter(
              (o) => !values.branchNotIn.includes(o)
            )}
            dataCy="ai-review-rule-form-branch-in"
            helperText={branchesHint}
            onChange={setScope('branchIn', 'branchNotIn')}
          />
          <ScopeAutocomplete
            label={t(
              'ai_review_rule_form_branches_except_label',
              'Except branches'
            )}
            value={values.branchNotIn}
            options={branchOptions.filter((o) => !values.branchIn.includes(o))}
            dataCy="ai-review-rule-form-branch-not-in"
            helperText={branchesHint}
            onChange={setScope('branchNotIn', 'branchIn')}
          />
        </StyledTwoColumns>
      )}
    </Box>
  );
};
