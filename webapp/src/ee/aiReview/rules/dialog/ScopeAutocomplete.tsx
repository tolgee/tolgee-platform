import { Autocomplete, Box, Chip, TextField } from '@mui/material';

import { FieldLabel } from 'tg.component/FormField';

type ScopeAutocompleteProps = {
  label: string;
  value: string[];
  options: string[];
  dataCy: string;
  helperText?: string;
  onChange: (next: string[]) => void;
};

export const ScopeAutocomplete = ({
  label,
  value,
  options,
  dataCy,
  helperText,
  onChange,
}: ScopeAutocompleteProps) => {
  return (
    <Box display="grid">
      <FieldLabel>{label}</FieldLabel>
      <Autocomplete
        multiple
        freeSolo
        size="small"
        options={options}
        value={value}
        onChange={(_, next) => onChange(next as string[])}
        renderOption={(props, option) => (
          <li
            {...props}
            key={option}
            data-cy="ai-review-rule-form-scope-option"
            data-cy-option={option}
          >
            {option}
          </li>
        )}
        renderTags={(items, getTagProps) =>
          items.map((item, index) => (
            // eslint-disable-next-line react/jsx-key
            <Chip size="small" label={item} {...getTagProps({ index })} />
          ))
        }
        renderInput={(params) => (
          <TextField
            {...params}
            size="small"
            helperText={helperText}
            inputProps={{
              ...params.inputProps,
              'data-cy': dataCy,
            }}
          />
        )}
      />
    </Box>
  );
};
