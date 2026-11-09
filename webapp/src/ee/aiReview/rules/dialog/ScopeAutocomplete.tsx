import { Autocomplete, Box, Chip, TextField } from '@mui/material';

import { FieldLabel } from 'tg.component/FormField';

type ScopeFieldProps = {
  label: string;
  value: string[];
  options: string[];
  dataCy: string;
  emptyValueLabel?: string;
  helperText?: string;
  onChange: (next: string[]) => void;
};

export const ScopeAutocomplete = ({
  label,
  value,
  options,
  dataCy,
  emptyValueLabel,
  helperText,
  onChange,
}: ScopeFieldProps) => {
  const optionLabel = (option: string) =>
    option === '' && emptyValueLabel ? emptyValueLabel : option;
  return (
    <Box display="grid">
      <FieldLabel>{label}</FieldLabel>
      <Autocomplete
        multiple
        freeSolo
        size="small"
        options={options}
        value={value}
        getOptionLabel={optionLabel}
        onChange={(_, next) => onChange(next as string[])}
        renderOption={(props, option) => (
          <li
            {...props}
            key={option}
            data-cy="ai-review-rule-form-scope-option"
            data-cy-option={option}
          >
            {optionLabel(option)}
          </li>
        )}
        renderTags={(items, getTagProps) =>
          items.map((item, index) => (
            // eslint-disable-next-line react/jsx-key
            <Chip
              size="small"
              label={optionLabel(item)}
              {...getTagProps({ index })}
            />
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
