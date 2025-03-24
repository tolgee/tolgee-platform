import { useMemo } from 'react';
import { Box, IconButton, styled, TextField, Typography } from '@mui/material';
import { ChevronDown, ChevronUp, Send03 } from '@untitled-ui/icons-react';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { EditorHandlebars } from 'tg.component/editor/EditorHandlebars';
import { EditorWrapper } from 'tg.component/editor/EditorWrapper';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';
import { FieldLabel } from 'tg.component/FormField';
import { PanelContentProps } from '../common/types';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { AiResult } from './AiResult';

const StyledTextField = styled(TextField)`
  flex-grow: 1;
  opacity: 0.5;
  &:focus-within {
    opacity: 1;
  }
  &:focus-within .icon-button {
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

export const AiPrompt: React.FC<PanelContentProps> = (props) => {
  const [value, setValue] = useLocalStorageState<string>({
    key: 'aiPlaygroundLastValue',
    initial: 'Hi translate from {{source}} to {{target}}',
  });
  const [expanded, setExpanded] = useLocalStorageState({
    key: 'aiPlaygroundExpanded',
    initial: undefined,
  });

  const promptLoadable = useApiMutation({
    url: '/v2/prompts/test',
    method: 'post',
  });

  const cellSelected = Boolean(props.keyData && props.language);

  const promptVariables = useApiQuery({
    url: '/v2/prompts/get-variables',
    method: 'get',
    query: {
      projectId: props.project.id,
      keyId: props.keyData?.keyId,
      targetLanguageId: props.language?.id,
    },
    options: {
      enabled: cellSelected,
    },
  });

  function handleTestPrompt() {
    if (!cellSelected) {
      return;
    }
    promptLoadable.mutate({
      content: {
        'application/json': {
          template: value,
          keyId: props.keyData.keyId,
          targetLanguageId: props.language.id,
          projectId: props.project.id,
        },
      },
    });
  }

  const jsonValue = useMemo(() => {
    try {
      return JSON.parse(promptLoadable.data?.result ?? '');
    } catch (e) {
      return undefined;
    }
  }, [promptLoadable.data?.result]);

  return (
    <Box display="grid">
      <Box sx={{ margin: '8px' }}>
        <FieldLabel>Prompt</FieldLabel>
        <EditorWrapper onKeyDown={stopBubble()}>
          <EditorHandlebars
            minHeight={100}
            value={value}
            onChange={setValue}
            unknownVariableMessage={
              cellSelected
                ? 'Unknown variable'
                : 'Select translation to see the value'
            }
            shortcuts={[
              {
                key: 'Mod-Enter',
                run: () => (handleTestPrompt(), true),
              },
            ]}
            availableVariables={promptVariables.data?.data}
          />
        </EditorWrapper>
      </Box>

      <Box sx={{ margin: '8px', display: 'flex', justifyContent: 'end' }}>
        <IconButton
          color="primary"
          onClick={handleTestPrompt}
          disabled={!cellSelected || promptLoadable.isLoading}
          size="medium"
        >
          {promptLoadable.isLoading ? (
            <SpinnerProgress size={22} />
          ) : (
            <Send03 width={22} height={22} />
          )}
        </IconButton>
      </Box>

      <Box sx={{ margin: '8px', display: 'grid' }}>
        <AiResult
          raw={promptLoadable.data?.result}
          json={jsonValue}
          isPlural={props.keyData?.keyIsPlural}
          locale={props.language?.tag}
        />

        <Typography variant="caption" minHeight={20}>
          {promptLoadable.data?.usage && (
            <>
              tokens: {promptLoadable.data.usage.total_tokens}, cached:{' '}
              {promptLoadable.data.usage.prompt_tokens_details.cached_tokens}
            </>
          )}
        </Typography>
      </Box>

      {Boolean(expanded) && (
        <Box sx={{ margin: '8px', display: 'grid' }}>
          <FieldLabel>Rendered prompt</FieldLabel>
          <StyledTextField
            multiline
            variant="outlined"
            size="small"
            value={promptLoadable.data?.prompt}
            onChange={(e) => e.preventDefault()}
            data-cy="translations-comments-output"
            InputProps={{
              sx: {
                padding: '8px 4px 8px 12px',
                borderRadius: '8px',
              },
            }}
          />
        </Box>
      )}

      <Box display="flex" justifyContent="center">
        <IconButton
          onClick={() => setExpanded((val) => (val ? undefined : 'true'))}
        >
          {expanded ? <ChevronUp /> : <ChevronDown />}
        </IconButton>
      </Box>
    </Box>
  );
};
