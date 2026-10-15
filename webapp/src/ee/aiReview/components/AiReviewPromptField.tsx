import { Link as MuiLink, MenuItem, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { Select } from 'tg.component/common/Select';
import { getAiPlaygroundUrl } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';

const StyledSection = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
`;

export type AiReviewPromptValue = number | 'default';

type Props = {
  projectId: number;
  value: AiReviewPromptValue;
  onChange: (value: AiReviewPromptValue) => void;
};

export const AiReviewPromptField = ({ projectId, value, onChange }: Props) => {
  const { t } = useTranslate();

  const promptsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/prompts',
    method: 'get',
    path: { projectId },
    query: { size: 1000, sort: ['name'], type: 'REVIEW' },
  });

  const reviewPrompts = promptsLoadable.data?._embedded?.prompts ?? [];

  return (
    <StyledSection>
      <Select
        label={t('ai_review_rule_prompt_label', 'Review prompt')}
        value={value}
        onChange={(e) =>
          onChange(
            e.target.value === 'default' ? 'default' : Number(e.target.value)
          )
        }
        minHeight={false}
        data-cy="ai-review-rule-prompt-select"
      >
        <MenuItem value="default">
          {t('ai_review_default_prompt', 'Default review prompt')}
        </MenuItem>
        {reviewPrompts.map((prompt) => (
          <MenuItem key={prompt.id} value={prompt.id}>
            {prompt.name}
          </MenuItem>
        ))}
      </Select>
      <MuiLink
        component={Link}
        to={getAiPlaygroundUrl(projectId, {
          promptId: value === 'default' ? undefined : value,
          type: 'review',
        })}
        variant="body2"
        data-cy="ai-review-rule-open-playground"
      >
        {t('ai_review_rule_open_prompt', 'Open in AI playground')}
      </MuiLink>
    </StyledSection>
  );
};
