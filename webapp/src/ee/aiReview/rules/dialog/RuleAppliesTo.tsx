import { useState } from 'react';
import { Autocomplete, Box, Chip, styled, TextField } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { useProject } from 'tg.hooks/useProject';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { FieldLabel } from 'tg.component/FormField';
import { useIsBranchingEnabled } from 'tg.component/branching/useIsBranchingEnabled';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';
import { RuleLanguagesField } from 'tg.ee.module/aiReview/rules/dialog/RuleLanguagesField';
import { RuleSummarySection } from 'tg.ee.module/aiReview/rules/dialog/RuleSummarySection';

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

export const RuleAppliesTo = () => {
  const { t } = useTranslate();
  const project = useProject();
  const isBranchingEnabled = useIsBranchingEnabled();
  const languages = useProjectLanguages();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();
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

  const withoutTags = t('ai_review_rule_form_tags_without', 'Without tags');
  const defaultNamespace = t(
    'ai_review_rule_form_namespaces_default',
    'Default namespace'
  );
  const tagLabel = (value: string) => (value === '' ? withoutTags : value);
  const namespaceLabel = (value: string) =>
    value === '' ? defaultNamespace : value;

  const filterParts: string[] = [];
  if (values.tagIn.length) {
    filterParts.push(
      t('ai_review_rule_summary_tags', 'Tags: {list}', {
        list: values.tagIn.map(tagLabel).join(', '),
      })
    );
  }
  if (values.tagNotIn.length) {
    filterParts.push(
      t('ai_review_rule_summary_tags_not', 'Except tags: {list}', {
        list: values.tagNotIn.map(tagLabel).join(', '),
      })
    );
  }
  if (values.namespaceIn.length) {
    filterParts.push(
      t('ai_review_rule_summary_namespaces', 'Namespaces: {list}', {
        list: values.namespaceIn.map(namespaceLabel).join(', '),
      })
    );
  }
  if (values.namespaceNotIn.length) {
    filterParts.push(
      t('ai_review_rule_summary_namespaces_not', 'Except namespaces: {list}', {
        list: values.namespaceNotIn.map(namespaceLabel).join(', '),
      })
    );
  }
  if (values.branchIn.length) {
    filterParts.push(
      t('ai_review_rule_summary_branches', 'Branches: {list}', {
        list: values.branchIn.join(', '),
      })
    );
  }
  if (values.branchNotIn.length) {
    filterParts.push(
      t('ai_review_rule_summary_branches_not', 'Except branches: {list}', {
        list: values.branchNotIn.join(', '),
      })
    );
  }

  const selectedLanguages = languages.filter((language) =>
    values.languageIds.includes(language.id)
  );
  const languagesPart = selectedLanguages.length
    ? selectedLanguages.map((language) => language.name).join(', ')
    : t('ai_review_rule_form_languages_all', 'All languages');
  const keysPart = filterParts.length
    ? t('ai_review_rule_summary_filtered_keys', 'Filtered keys')
    : t('ai_review_rule_summary_all_keys', 'All keys');

  return (
    <RuleSummarySection
      label={t('ai_review_rule_form_applies_to_title', 'Applies to')}
      primary={t('ai_review_rule_summary_applies', '{keys} • {languages}', {
        keys: keysPart,
        languages: languagesPart,
      })}
      secondaryLines={
        filterParts.length
          ? filterParts
          : [t('ai_review_rule_summary_no_filters', 'No filters')]
      }
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
            emptyValueLabel={withoutTags}
            onChange={setScope('tagIn', 'tagNotIn')}
          />
          <ScopeAutocomplete
            label={t('ai_review_rule_form_tags_except_label', 'Except tags')}
            value={values.tagNotIn}
            options={tagOptions.filter((o) => !values.tagIn.includes(o))}
            dataCy="ai-review-rule-form-tag-not-in"
            emptyValueLabel={withoutTags}
            onChange={setScope('tagNotIn', 'tagIn')}
          />
          <ScopeAutocomplete
            label={t('ai_review_rule_form_namespaces_label', 'Namespaces')}
            value={values.namespaceIn}
            options={namespaceOptions.filter(
              (o) => !values.namespaceNotIn.includes(o)
            )}
            dataCy="ai-review-rule-form-namespace-in"
            emptyValueLabel={defaultNamespace}
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
            emptyValueLabel={defaultNamespace}
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
    </RuleSummarySection>
  );
};
