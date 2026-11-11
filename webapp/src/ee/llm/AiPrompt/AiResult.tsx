import React from 'react';
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
import { Label } from 'tg.ee.module/llm/AiPrompt/Label';

const StyledPre = styled(Box)`
  flex-grow: 1;
  color: ${({ theme }) => theme.palette.text.secondary};
  white-space: pre-wrap;
  word-break: break-word;
`;

export const StyledDescription = styled('div')`
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  raw: string | undefined;
  loading: boolean;
  parsed: React.ReactNode | undefined;
};

export const AiResult = ({ raw, loading, parsed }: Props) => {
  const { t } = useTranslate();
  const [storedMode, setMode] = useLocalStorageState({
    key: 'aiPlaygroundResultMode',
    initial: 'translation',
  });

  const hasParsedResult = parsed != null;
  const showParsed = hasParsedResult && storedMode !== 'raw';

  return (
    <Box display="grid">
      <Label
        rightContent={
          <Tooltip
            title={
              showParsed
                ? t('ai_prompt_show_result_raw')
                : t('ai_prompt_show_result_translation')
            }
          >
            <span>
              <IconButton
                sx={{ marginY: -1 }}
                color={showParsed ? undefined : 'primary'}
                disabled={!hasParsedResult}
                onClick={() => setMode(showParsed ? 'raw' : 'translation')}
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
      ) : !raw && !hasParsedResult ? (
        <StyledDescription sx={{ fontStyle: 'italic' }}>
          {t('ai_playground_result_empty')}
        </StyledDescription>
      ) : !showParsed ? (
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
      ) : (
        parsed
      )}
    </Box>
  );
};
