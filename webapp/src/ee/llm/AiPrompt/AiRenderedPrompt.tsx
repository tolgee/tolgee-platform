import { Box, IconButton, Skeleton, styled } from '@mui/material';
import { Label } from './Label';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';
import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';

const StyledDescription = styled(Box)`
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
  font-style: italic;
`;

const StyledContent = styled(Box)`
  border-radius: 4px;
  color: ${({ theme }) => theme.palette.text.secondary};
  border: 1px solid
    ${({ theme }) =>
      theme.palette.tokens._components.input.outlined.enabledBorder};
  background: ${({ theme }) => theme.palette.tokens.text._states.hover};
  padding: 12px;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  contain: inline-size;
`;

type Props = {
  data: string | undefined;
  loading: boolean;
};

export const AiRenderedPrompt = ({ data, loading }: Props) => {
  const [expanded, setExpanded] = useLocalStorageState({
    key: 'aiPlaygroundExpanded',
    initial: undefined,
  });
  const { t } = useTranslate();
  return (
    <>
      <Label
        rightContent={
          <IconButton
            sx={{ marginY: -1 }}
            onClick={() => setExpanded((val) => (val ? undefined : 'true'))}
            data-cy="ai-prompt-rendered-expand-button"
          >
            {expanded ? (
              <ChevronUp height={20} width={20} />
            ) : (
              <ChevronDown height={20} width={20} />
            )}
          </IconButton>
        }
      >
        {t('ai_prompt_rendered_label')}
      </Label>
      {Boolean(expanded) &&
        (loading ? (
          <StyledDescription>
            <Skeleton variant="text" sx={{ fontSize: 72 }} />
          </StyledDescription>
        ) : !data ? (
          <StyledDescription>
            {t('ai_playground_rendered_prompt_empty')}
          </StyledDescription>
        ) : (
          <StyledContent data-cy="ai-prompt-rendered">{data}</StyledContent>
        ))}
    </>
  );
};
