import { Autocomplete, Box, Chip, TextField, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { StyledInputLabel } from 'tg.component/common/TextField';
import { StyledFields } from 'tg.ee.module/aiReview/components/AiReviewScopeField';
import { useBranchesService } from 'tg.views/projects/translations/context/services/useBranchesService';

export type AiReviewBranchValue = {
  branchIn: string[];
  branchNotIn: string[];
};

type Props = {
  projectId: number;
  value: AiReviewBranchValue;
  onChange: (value: AiReviewBranchValue) => void;
};

export const AiReviewBranchField = ({ projectId, value, onChange }: Props) => {
  const { t } = useTranslate();
  const { branches } = useBranchesService({ projectId });
  const options = branches.map((b) => b.name);

  const patternInput = (
    patterns: string[],
    setPatterns: (patterns: string[]) => void,
    placeholder: string,
    kind: 'include' | 'exclude'
  ) => (
    <Autocomplete
      multiple
      freeSolo
      autoSelect
      size="small"
      options={options}
      value={patterns}
      onChange={(_, next) =>
        setPatterns(
          (next as string[]).map((p) => p.trim()).filter((p) => p.length > 0)
        )
      }
      renderTags={(items, getTagProps) =>
        items.map((item, index) => (
          // eslint-disable-next-line react/jsx-key
          <Chip size="small" label={item} {...getTagProps({ index })} />
        ))
      }
      renderInput={(params) => (
        <TextField
          {...params}
          placeholder={patterns.length ? undefined : placeholder}
          data-cy="ai-review-rule-branch-input"
          data-cy-kind={kind}
        />
      )}
    />
  );

  return (
    <Box display="grid" gap={0.5}>
      <StyledFields data-cy="ai-review-rule-branches">
        <Box className="field">
          <StyledInputLabel>
            {t('ai_review_rule_branches_label', 'Branches')}
          </StyledInputLabel>
          {patternInput(
            value.branchIn,
            (branchIn) => onChange({ ...value, branchIn }),
            t('ai_review_rule_branches_all', 'All branches'),
            'include'
          )}
        </Box>
        <Box className="field">
          <StyledInputLabel>
            {t('ai_review_rule_branches_except_label', 'Except branches')}
          </StyledInputLabel>
          {patternInput(
            value.branchNotIn,
            (branchNotIn) => onChange({ ...value, branchNotIn }),
            t('ai_review_rule_branches_none', 'None'),
            'exclude'
          )}
        </Box>
      </StyledFields>
      <Typography variant="caption" color="text.secondary">
        {t(
          'ai_review_rule_branches_hint',
          'Type a branch name or a pattern like feature/*'
        )}
      </Typography>
    </Box>
  );
};
