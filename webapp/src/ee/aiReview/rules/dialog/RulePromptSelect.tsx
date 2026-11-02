import {
  Box,
  FormControl,
  InputLabel,
  Link as MuiLink,
  MenuItem,
  Select,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';
import { Link } from 'react-router-dom';

import { getAiPlaygroundUrl } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import {
  RuleFormValues,
  ruleNeverFullyApplies,
} from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

export const RulePromptSelect = () => {
  const { t } = useTranslate();
  const project = useProject();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();

  const promptsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/prompts',
    method: 'get',
    path: { projectId: project.id },
    query: { type: 'REVIEW', size: 1000 },
  });

  const prompts = promptsLoadable.data?._embedded?.prompts ?? [];
  const label = t('ai_review_rule_form_prompt_label', 'Review prompt');
  const disabled = ruleNeverFullyApplies(values);

  return (
    <Box display="grid" gap={0.5} justifyItems="start">
      <FormControl size="small" disabled={disabled} fullWidth>
        <InputLabel shrink>{label}</InputLabel>
        <Select
          notched
          label={label}
          value={values.promptId}
          displayEmpty
          onChange={(e) =>
            setFieldValue(
              'promptId',
              e.target.value === '' ? '' : Number(e.target.value)
            )
          }
          SelectDisplayProps={
            { 'data-cy': 'ai-review-rule-form-prompt' } as any
          }
        >
          <MenuItem value="">
            {t('ai_review_rule_form_prompt_default', 'Default review prompt')}
          </MenuItem>
          {prompts.map((prompt) => (
            <MenuItem
              key={prompt.id}
              value={prompt.id}
              data-cy="ai-review-rule-form-prompt-item"
            >
              {prompt.name}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      <MuiLink
        component={Link}
        to={getAiPlaygroundUrl(
          project.id,
          values.promptId === '' ? undefined : values.promptId,
          undefined,
          'REVIEW'
        )}
        variant="body2"
        data-cy="ai-review-rule-form-prompt-playground"
      >
        {t('ai_review_rule_form_prompt_playground', 'Open in AI playground')}
      </MuiLink>
    </Box>
  );
};
