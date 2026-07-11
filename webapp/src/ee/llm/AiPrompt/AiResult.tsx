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
import { Label } from './Label';
import { AiPlaygroundPreview } from 'tg.views/projects/translations/translationVisual/AiPlaygroundPreview';

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

type Props = {
  raw: string | undefined;
  json: Record<string, string | undefined> | undefined;
  isPlural: boolean;
  locale: string;
  loading: boolean;
};

export const AiResult = ({ raw, json, isPlural, locale, loading }: Props) => {
  const { t } = useTranslate();
  const [_mode, setMode] = useLocalStorageState({
    key: 'aiPlaygroundResultMode',
    initial: 'translation',
  });

  const mode = json?.output ? _mode : 'raw';

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
                disabled={!json?.output}
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
      ) : !raw && !json ? (
        <StyledDescription sx={{ fontStyle: 'italic' }}>
          {t('ai_playground_result_empty')}
        </StyledDescription>
      ) : mode === 'raw' ? (
        <Box display="grid" gap={1}>
          <StyledPre data-cy="ai-prompt-result-raw">{raw}</StyledPre>
          {!json?.output && (
            <Box display="grid">
              <Alert color="warning" icon={false}>
                <T keyName="ai_playground_result_not_json_warning" />
              </Alert>
            </Box>
          )}
        </Box>
      ) : (
        <Box display="grid" gap={1} data-cy="ai-prompt-result-translation">
          <AiPlaygroundPreview
            locale={locale}
            isPlural={isPlural}
            translation={json?.output}
          />
          {json?.contextDescription && (
            <StyledDescription>{json?.contextDescription}</StyledDescription>
          )}
        </Box>
      )}
    </Box>
  );
};
