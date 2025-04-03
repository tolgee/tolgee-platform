import { Box, Button, ButtonGroup, styled, TextField } from '@mui/material';
import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';
import { FieldLabel } from 'tg.component/FormField';
import { TranslationVisual } from 'tg.views/projects/translations/translationVisual/TranslationVisual';

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

const StyledDescription = styled('div')`
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  raw: string | undefined;
  json: any | undefined;
  isPlural: boolean;
  locale: string;
};

export const AiResult = ({ raw, json, isPlural, locale }: Props) => {
  const [_mode, setMode] = useLocalStorageState({
    key: 'aiPlaygroundResultMode',
    initial: 'translation',
  });

  const mode = json?.output ? _mode : 'raw';

  return (
    <Box display="grid" gap={1}>
      <Box display="flex" justifyContent="space-between" alignItems="end">
        <FieldLabel sx={{ margin: 0 }}>Result</FieldLabel>
        <ButtonGroup disabled={!json?.output}>
          <Button
            size="small"
            disableElevation
            color={mode === 'translation' ? 'primary' : 'default'}
            onClick={() => setMode('translation')}
          >
            Translation
          </Button>
          <Button
            size="small"
            disableElevation
            color={mode === 'raw' ? 'primary' : 'default'}
            onClick={() => setMode('raw')}
            data-cy="invitation-dialog-type-link-button"
          >
            Raw
          </Button>
        </ButtonGroup>
      </Box>
      {mode === 'raw' ? (
        <StyledTextField
          multiline
          minRows={3}
          variant="outlined"
          size="small"
          value={raw}
          onChange={(e) => e.preventDefault()}
          data-cy="translations-comments-output"
          InputProps={{
            sx: {
              padding: '8px 4px 8px 12px',
              borderRadius: '8px',
            },
          }}
        />
      ) : (
        <Box
          border="1px solid lightgray"
          borderRadius="4px"
          padding="4px 8px 8px 8px"
          display="grid"
          gap={1}
        >
          <TranslationVisual
            maxLines={100}
            text={json.output ?? ''}
            locale={locale}
            isPlural={isPlural}
          />
          {json.contextDescription && (
            <StyledDescription>{json.contextDescription}</StyledDescription>
          )}
        </Box>
      )}
    </Box>
  );
};
