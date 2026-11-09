import {
  Alert,
  Box,
  IconButton,
  Skeleton,
  styled,
  Tooltip,
} from '@mui/material';
import { Code02 } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';
import { components } from 'tg.service/apiSchema.generated';
import { Label } from './Label';
import { AiPlaygroundPreview } from 'tg.views/projects/translations/translationVisual/AiPlaygroundPreview';
import { TranslationVisual } from 'tg.views/projects/translations/translationVisual/TranslationVisual';
import { AiReviewPlaygroundResult } from 'tg.ee.module/aiReview/translations/AiReviewPlaygroundResult';
import { PromptType } from './PromptTypeToggle';

const StyledPre = styled(Box)`
  flex-grow: 1;
  color: ${({ theme }) => theme.palette.text.secondary};
  white-space: pre-wrap;
  word-break: break-word;
`;

const StyledDescription = styled('div')`
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type AiReviewIssueModel = components['schemas']['AiReviewIssueModel'];

type Props = {
  raw: string | undefined;
  json: Record<string, string | undefined> | undefined;
  isPlural: boolean;
  locale: string;
  loading: boolean;
  promptType?: PromptType;
  issues?: AiReviewIssueModel[] | null;
  score?: number | null;
};

export const AiResult = ({
  raw,
  json,
  isPlural,
  locale,
  loading,
  promptType,
  issues,
  score,
}: Props) => {
  const { t } = useTranslate();
  const [_mode, setMode] = useLocalStorageState({
    key: 'aiPlaygroundResultMode',
    initial: 'translation',
  });

  const isReview = promptType === 'REVIEW';
  const hasParsedResult = isReview ? score != null : Boolean(json?.output);
  const mode = hasParsedResult ? _mode : 'raw';

  return (
    <Box display="grid">
      <Label
        rightContent={
          <Tooltip
            title={
              mode === 'translation'
                ? t('ai_prompt_show_result_raw')
                : t('ai_prompt_show_result_translation')
            }
          >
            <span>
              <IconButton
                sx={{ marginY: -1 }}
                color={mode === 'translation' ? undefined : 'primary'}
                disabled={!hasParsedResult}
                onClick={() =>
                  setMode(_mode === 'translation' ? 'raw' : 'translation')
                }
                data-cy="ai-prompt-show-result-toggle"
              >
                <Code02 height={20} width={20} />
              </IconButton>
            </span>
          </Tooltip>
        }
      >
        {t('ai_prompt_result_label')}
      </Label>
      {loading ? (
        <>
          <Skeleton variant="text" sx={{ fontSize: 36 }} />
          <Skeleton variant="text" sx={{ width: '75%' }} />
        </>
      ) : !raw && !json && !hasParsedResult ? (
        <StyledDescription sx={{ fontStyle: 'italic' }}>
          {t('ai_playground_result_empty')}
        </StyledDescription>
      ) : mode === 'raw' ? (
        <Box display="grid" gap={1}>
          <StyledPre data-cy="ai-prompt-result-raw">{raw}</StyledPre>
          {!hasParsedResult && (
            <Box display="grid">
              <Alert color="warning" icon={false}>
                <T keyName="ai_playground_result_not_json_warning" />
              </Alert>
            </Box>
          )}
        </Box>
      ) : isReview && score != null ? (
        <Box display="grid" gap={1} data-cy="ai-prompt-result-review">
          <AiPlaygroundPreview>
            <AiReviewPlaygroundResult
              score={score}
              issues={issues ?? undefined}
            />
          </AiPlaygroundPreview>
        </Box>
      ) : (
        <Box display="grid" gap={1} data-cy="ai-prompt-result-translation">
          <AiPlaygroundPreview>
            <TranslationVisual
              text={json?.output}
              isPlural={isPlural}
              locale={locale}
              maxLines={3}
              extraPadding={false}
            />
          </AiPlaygroundPreview>
          {json?.contextDescription && (
            <StyledDescription>{json?.contextDescription}</StyledDescription>
          )}
        </Box>
      )}
    </Box>
  );
};
