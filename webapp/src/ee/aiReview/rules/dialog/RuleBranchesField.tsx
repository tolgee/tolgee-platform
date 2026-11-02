import { Autocomplete, Chip, TextField } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type Props = {
  branchIn: string[];
  branchNotIn: string[];
  onChange: (value: { branchIn: string[]; branchNotIn: string[] }) => void;
};

type FieldProps = {
  label: string;
  value: string[];
  options: string[];
  dataCy: string;
  helperText: string;
  onChange: (next: string[]) => void;
};

const BranchAutocomplete = ({
  label,
  value,
  options,
  dataCy,
  helperText,
  onChange,
}: FieldProps) => (
  <Autocomplete
    multiple
    freeSolo
    options={options}
    value={value}
    onChange={(_, next) => onChange(next as string[])}
    renderTags={(tags, getTagProps) =>
      tags.map((tag, index) => (
        // eslint-disable-next-line react/jsx-key
        <Chip size="small" label={tag} {...getTagProps({ index })} />
      ))
    }
    renderInput={(params) => (
      <TextField
        {...params}
        size="small"
        label={label}
        helperText={helperText}
        inputProps={{
          ...params.inputProps,
          'data-cy': dataCy,
        }}
      />
    )}
  />
);

export const RuleBranchesField = ({
  branchIn,
  branchNotIn,
  onChange,
}: Props) => {
  const { t } = useTranslate();
  const project = useProject();

  const branchesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/branches',
    method: 'get',
    path: { projectId: project.id },
    query: { page: 0, size: 1000 },
  });

  const branchNames =
    branchesLoadable.data?._embedded?.branches?.map((branch) => branch.name) ??
    [];

  const helperText = t(
    'ai_review_rule_form_branches_hint',
    'Type a branch name or a pattern like feature/*'
  );

  return (
    <>
      <BranchAutocomplete
        label={t('ai_review_rule_form_branches_label', 'Branches')}
        value={branchIn}
        options={branchNames}
        dataCy="ai-review-rule-form-branch-in"
        helperText={helperText}
        onChange={(next) => onChange({ branchIn: next, branchNotIn })}
      />
      <BranchAutocomplete
        label={t(
          'ai_review_rule_form_branches_except_label',
          'Except branches'
        )}
        value={branchNotIn}
        options={branchNames}
        dataCy="ai-review-rule-form-branch-not-in"
        helperText={helperText}
        onChange={(next) => onChange({ branchIn, branchNotIn: next })}
      />
    </>
  );
};
