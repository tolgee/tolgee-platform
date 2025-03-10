import { useApiMutation } from 'tg.service/http/useQueryApi';
import { PanelContentProps } from '../../common/types';
import { Box, IconButton, styled, TextField } from '@mui/material';
import { useState } from 'react';
import { Send03 } from '@untitled-ui/icons-react';

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

export const Prompt: React.FC<PanelContentProps> = (props) => {
  const [value, setValue] = useState('');
  const [result, setResult] = useState('');
  const promptLoadable = useApiMutation({
    url: '/v2/prompts/test',
    method: 'post',
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

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (
      !e.altKey &&
      !e.ctrlKey &&
      !e.metaKey &&
      !e.shiftKey &&
      !promptLoadable.isLoading
    ) {
      if (e.key === 'Enter') {
        handleTestPrompt();
        e.preventDefault();
      } else if (e.key === 'Escape') {
        e.preventDefault();
      }
    }
  };

  return (
    <Box display="grid">
      <StyledTextField
        multiline
        variant="outlined"
        size="small"
        value={value}
        onChange={(e) => setValue(e.currentTarget.value)}
        onKeyDown={handleKeyDown}
        data-cy="translations-comments-input"
        InputProps={{
          sx: {
            padding: '8px 4px 8px 12px',
            borderRadius: '8px',
          },
          endAdornment: (
            <IconButton
              className="icon-button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={handleTestPrompt}
              disabled={promptLoadable.isLoading}
              sx={{ my: '-6px', alignSelf: 'end' }}
            >
              <Send03 width={20} height={20} color="inherit" />
            </IconButton>
          ),
        }}
      />

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
