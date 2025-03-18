import { useState } from 'react';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { Box, IconButton, styled, TextField } from '@mui/material';
import { Send03 } from '@untitled-ui/icons-react';

import { EditorHandlebars } from 'tg.component/editor/EditorHandlebars';
import { EditorWrapper } from 'tg.component/editor/EditorWrapper';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { PanelContentProps } from '../common/types';

const StyledTextField = styled(TextField)`
  flex-grow: 1;
  margin: 8px;
  opacity: 0.5;
  &:focus-within {
    opacity: 1;
  }
  &:focus-within .icon-button {
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

export const AiPrompt: React.FC<PanelContentProps> = (props) => {
  const [value, setValue] = useState(
    'Hi translate from {{source}} to {{target}}'
  );
  const [result, setResult] = useState('');
  const promptLoadable = useApiMutation({
    url: '/v2/prompts/test',
    method: 'post',
  });

  const promptVariables = useApiQuery({
    url: '/v2/prompts/get-variables',
    method: 'get',
    query: {
      pId: props.project.id,
      keyId: props.keyData.keyId,
      targetLanguageId: props.language.id,
    },
  });

  function handleTestPrompt() {
    promptLoadable.mutate(
      {
        content: {
          'application/json': {
            template: value,
            keyId: props.keyData.keyId,
            targetLanguageId: props.language.id,
            projectId: props.project.id,
          },
        },
      },
      {
        onSuccess(data) {
          setResult(data.prompt);
        },
      }
    );
  }

  return (
    <Box display="grid">
      <EditorWrapper sx={{ margin: '8px' }} onKeyDown={stopBubble()}>
        <EditorHandlebars
          minHeight={100}
          value={value}
          onChange={setValue}
          shortcuts={[
            {
              key: 'Mod-Enter',
              run: () => (handleTestPrompt(), true),
            },
          ]}
          availableVariables={promptVariables.data?.data}
        />
      </EditorWrapper>

      <Box sx={{ margin: '8px', display: 'flex', justifyContent: 'end' }}>
        <IconButton color="primary" onClick={handleTestPrompt}>
          <Send03 width={20} height={20} />
        </IconButton>
      </Box>

      <StyledTextField
        multiline
        variant="outlined"
        size="small"
        value={result}
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
  );
};
