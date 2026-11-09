import { useState } from 'react';
import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';
import { RuleLanguagesField } from 'tg.ee.module/aiReview/rules/dialog/RuleLanguagesField';
import { CollapsibleSummaryRow } from 'tg.component/common/CollapsibleSummaryRow';
import { ScopeAutocomplete } from 'tg.ee.module/aiReview/rules/dialog/ScopeAutocomplete';
import { useRuleScopeOptions } from 'tg.ee.module/aiReview/rules/dialog/useRuleScopeOptions';
import { useAppliesToSummary } from 'tg.ee.module/aiReview/rules/dialog/useAppliesToSummary';

const StyledTwoColumns = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: ${({ theme }) => theme.spacing(2)};
  align-items: start;
`;

export const RuleAppliesTo = () => {
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();
  const {
    isBranchingEnabled,
    tagOptions,
    namespaceOptions,
    branchOptions,
    withoutTagsLabel,
    defaultNamespaceLabel,
  } = useRuleScopeOptions();
  const summary = useAppliesToSummary(values, {
    withoutTagsLabel,
    defaultNamespaceLabel,
  });
  const [expanded, setExpanded] = useState(
    values.tagIn.length > 0 ||
      values.tagNotIn.length > 0 ||
      values.namespaceIn.length > 0 ||
      values.namespaceNotIn.length > 0 ||
      values.branchIn.length > 0 ||
      values.branchNotIn.length > 0 ||
      values.languageIds.length > 0
  );

  function setScope(
    field: keyof RuleFormValues,
    siblingField: keyof RuleFormValues
  ) {
    return (next: string[]) => {
      setFieldValue(field, next);
      const sibling = values[siblingField] as string[];
      const conflictFree = sibling.filter((item) => !next.includes(item));
      if (conflictFree.length !== sibling.length) {
        setFieldValue(siblingField, conflictFree);
      }
    };
  }

  const branchesHint = t(
    'ai_review_rule_form_branches_hint',
    'Type a branch name or a pattern like feature/*'
  );

  return (
    <CollapsibleSummaryRow
      label={t('ai_review_rule_form_applies_to_title', 'Applies to')}
      primary={summary.primary}
      secondaryLines={summary.secondaryLines}
      expanded={expanded}
      onToggle={() => setExpanded(!expanded)}
      dataCy="ai-review-rule-form-keys-filter-toggle"
    >
      <Box display="grid" gap={1.5} data-cy="ai-review-rule-form-keys-filter">
        <RuleLanguagesField />
        <StyledTwoColumns>
          <ScopeAutocomplete
            label={t('ai_review_rule_form_tags_label', 'Tags')}
            value={values.tagIn}
            options={tagOptions.filter((o) => !values.tagNotIn.includes(o))}
            dataCy="ai-review-rule-form-tag-in"
            emptyValueLabel={withoutTagsLabel}
            onChange={setScope('tagIn', 'tagNotIn')}
          />
          <ScopeAutocomplete
            label={t('ai_review_rule_form_tags_except_label', 'Except tags')}
            value={values.tagNotIn}
            options={tagOptions.filter((o) => !values.tagIn.includes(o))}
            dataCy="ai-review-rule-form-tag-not-in"
            emptyValueLabel={withoutTagsLabel}
            onChange={setScope('tagNotIn', 'tagIn')}
          />
          <ScopeAutocomplete
            label={t('ai_review_rule_form_namespaces_label', 'Namespaces')}
            value={values.namespaceIn}
            options={namespaceOptions.filter(
              (o) => !values.namespaceNotIn.includes(o)
            )}
            dataCy="ai-review-rule-form-namespace-in"
            emptyValueLabel={defaultNamespaceLabel}
            onChange={setScope('namespaceIn', 'namespaceNotIn')}
          />
          <ScopeAutocomplete
            label={t(
              'ai_review_rule_form_namespaces_except_label',
              'Except namespaces'
            )}
            value={values.namespaceNotIn}
            options={namespaceOptions.filter(
              (o) => !values.namespaceIn.includes(o)
            )}
            dataCy="ai-review-rule-form-namespace-not-in"
            emptyValueLabel={defaultNamespaceLabel}
            onChange={setScope('namespaceNotIn', 'namespaceIn')}
          />
          {isBranchingEnabled && (
            <>
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
                options={branchOptions.filter(
                  (o) => !values.branchIn.includes(o)
                )}
                dataCy="ai-review-rule-form-branch-not-in"
                helperText={branchesHint}
                onChange={setScope('branchNotIn', 'branchIn')}
              />
            </>
          )}
        </StyledTwoColumns>
      </Box>
    </CollapsibleSummaryRow>
  );
};
