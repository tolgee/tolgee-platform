import { useState } from 'react';
import {
  Autocomplete,
  Box,
  Checkbox,
  Chip,
  Collapse,
  FormControlLabel,
  styled,
  TextField,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { FieldLabel } from 'tg.component/FormField';
import { useIsBranchingEnabled } from 'tg.component/branching/useIsBranchingEnabled';
import {
  RuleFormValues,
  ruleNeverFullyApplies,
} from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';
import { RuleSectionToggle } from 'tg.ee.module/aiReview/rules/dialog/RuleSectionToggle';
import { RuleLanguagesField } from 'tg.ee.module/aiReview/rules/dialog/RuleLanguagesField';

const StyledTwoColumns = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: ${({ theme }) => theme.spacing(2)};
  align-items: start;
`;

type ScopeFieldProps = {
  label: string;
  value: string[];
  options: string[];
  dataCy: string;
  emptyValueLabel?: string;
  helperText?: string;
  onChange: (next: string[]) => void;
};

const ScopeAutocomplete = ({
  label,
  value,
  options,
  dataCy,
  emptyValueLabel,
  helperText,
  onChange,
}: ScopeFieldProps) => {
  const optionLabel = (option: string) =>
    option === '' && emptyValueLabel ? emptyValueLabel : option;
  return (
    <Box display="grid">
      <FieldLabel>{label}</FieldLabel>
      <Autocomplete
        multiple
        freeSolo
        size="small"
        options={options}
        value={value}
        getOptionLabel={optionLabel}
        onChange={(_, next) => onChange(next as string[])}
        renderTags={(items, getTagProps) =>
          items.map((item, index) => (
            // eslint-disable-next-line react/jsx-key
            <Chip
              size="small"
              label={optionLabel(item)}
              {...getTagProps({ index })}
            />
          ))
        }
        renderInput={(params) => (
          <TextField
            {...params}
            size="small"
            helperText={helperText}
            inputProps={{
              ...params.inputProps,
              'data-cy': dataCy,
            }}
          />
        )}
      />
    </Box>
  );
};

export const RuleKeysFilter = () => {
  const { t } = useTranslate();
  const project = useProject();
  const isBranchingEnabled = useIsBranchingEnabled();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();
  const fullNever = ruleNeverFullyApplies(values);
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

  const tagsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tags',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });
  const tagOptions = [
    '',
    ...(tagsLoadable.data?._embedded?.tags?.map((tag) => tag.name) ?? []),
  ];

  const namespacesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/used-namespaces',
    method: 'get',
    path: { projectId: project.id },
  });
  const namespaceOptions = [
    '',
    ...(namespacesLoadable.data?._embedded?.namespaces
      ?.map((namespace) => namespace.name ?? '')
      .filter((name) => name !== '') ?? []),
  ];

  const branchesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/branches',
    method: 'get',
    path: { projectId: project.id },
    query: { page: 0, size: 1000 },
    options: { enabled: isBranchingEnabled },
  });
  const branchOptions =
    branchesLoadable.data?._embedded?.branches?.map((branch) => branch.name) ??
    [];

  const branchesHint = t(
    'ai_review_rule_form_branches_hint',
    'Type a branch name or a pattern like feature/*'
  );

  return (
    <Box display="grid" gap={1.5} data-cy="ai-review-rule-form-keys-filter">
      <RuleSectionToggle
        title={t('ai_review_rule_form_keys_filter_title', 'Keys filter')}
        expanded={expanded}
        onToggle={() => setExpanded(!expanded)}
        dataCy="ai-review-rule-form-keys-filter-toggle"
      />
      <Collapse in={expanded}>
        <Box display="grid" gap={1.5} paddingTop={0.5}>
          <RuleLanguagesField />
          <StyledTwoColumns>
            <ScopeAutocomplete
              label={t('ai_review_rule_form_tags_label', 'Tags')}
              value={values.tagIn}
              options={tagOptions.filter((o) => !values.tagNotIn.includes(o))}
              dataCy="ai-review-rule-form-tag-in"
              emptyValueLabel={t(
                'ai_review_rule_form_tags_without',
                'Without tags'
              )}
              onChange={setScope('tagIn', 'tagNotIn')}
            />
            <ScopeAutocomplete
              label={t('ai_review_rule_form_tags_except_label', 'Except tags')}
              value={values.tagNotIn}
              options={tagOptions.filter((o) => !values.tagIn.includes(o))}
              dataCy="ai-review-rule-form-tag-not-in"
              emptyValueLabel={t(
                'ai_review_rule_form_tags_without',
                'Without tags'
              )}
              onChange={setScope('tagNotIn', 'tagIn')}
            />
            <ScopeAutocomplete
              label={t('ai_review_rule_form_namespaces_label', 'Namespaces')}
              value={values.namespaceIn}
              options={namespaceOptions.filter(
                (o) => !values.namespaceNotIn.includes(o)
              )}
              dataCy="ai-review-rule-form-namespace-in"
              emptyValueLabel={t(
                'ai_review_rule_form_namespaces_default',
                'Default namespace'
              )}
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
              emptyValueLabel={t(
                'ai_review_rule_form_namespaces_default',
                'Default namespace'
              )}
              onChange={setScope('namespaceNotIn', 'namespaceIn')}
            />
            {isBranchingEnabled && (
              <>
                <ScopeAutocomplete
                  label={t('ai_review_rule_form_branches_label', 'Branches')}
                  value={values.branchIn}
                  options={branchOptions}
                  dataCy="ai-review-rule-form-branch-in"
                  helperText={branchesHint}
                  onChange={(next) => setFieldValue('branchIn', next)}
                />
                <ScopeAutocomplete
                  label={t(
                    'ai_review_rule_form_branches_except_label',
                    'Except branches'
                  )}
                  value={values.branchNotIn}
                  options={branchOptions}
                  dataCy="ai-review-rule-form-branch-not-in"
                  helperText={branchesHint}
                  onChange={(next) => setFieldValue('branchNotIn', next)}
                />
              </>
            )}
          </StyledTwoColumns>
          {project.useQaChecks && (
            <FormControlLabel
              control={
                <Checkbox
                  checked={values.skipOpenQaIssues}
                  disabled={fullNever}
                  onChange={(_, checked) =>
                    setFieldValue('skipOpenQaIssues', checked)
                  }
                  data-cy="ai-review-rule-form-skip-qa"
                />
              }
              label={t(
                'ai_review_rule_form_skip_qa_label',
                "Don't auto-review translations with open QA issues"
              )}
            />
          )}
        </Box>
      </Collapse>
    </Box>
  );
};
