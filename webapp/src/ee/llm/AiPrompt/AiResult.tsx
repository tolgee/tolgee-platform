import { Box, IconButton, Skeleton, styled, Tooltip } from '@mui/material';
import { Code02 } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';

import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';
import { Label } from './Label';
import { AiPlaygroundPreview } from 'tg.views/projects/translations/translationVisual/AiPlaygroundPreview';

const StyledPre = styled(Box)`
  flex-grow: 1;
  color: ${({ theme }) => theme.palette.text.secondary};
  white-space: pre-wrap;
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
        <StyledDescription>
          <Skeleton variant="text" />
        </StyledDescription>
      ) : !raw && !json ? (
        <StyledDescription sx={{ fontStyle: 'italic' }}>
          {t('ai_playground_result_empty')}
        </StyledDescription>
      ) : mode === 'raw' ? (
        <StyledPre>{raw}</StyledPre>
      ) : (
        <Box display="grid" gap={1}>
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
