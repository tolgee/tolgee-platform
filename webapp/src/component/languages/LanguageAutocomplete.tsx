import React, { FC, ReactNode, useState } from 'react';
import { Box, IconButton, InputAdornment, TextField } from '@mui/material';
import { Add, Clear } from '@mui/icons-material';
import { Autocomplete } from '@mui/material';
import { suggest } from '@tginternal/language-util';
import { SuggestResult } from '@tginternal/language-util/lib/suggesting';
import { T } from '@tolgee/react';
import { MenuItem } from '@mui/material';

export type AutocompleteOption = Omit<SuggestResult, 'languageId'> & {
  isNew?: true;
  languageId: string;
  customRenderOption?: ReactNode;
};

const getOptions = (input: string): AutocompleteOption[] => {
  if (!input) {
    return suggest(input);
  }

  const newLang: AutocompleteOption = {
    isNew: true,
    flags: ['🏳️'],
    languageId: 'custom',
    originalName: '',
    englishName: input,
    customRenderOption: (
      <Box display="inline-flex" justifyContent="center">
        <Box mr={1}>
          <Add />
        </Box>
        <T>language_field_autocomplete_label_new_language</T>
        &nbsp;🏳️
      </Box>
    ),
  };
  return [newLang, ...suggest(input)];
};

export const LanguageAutocomplete: FC<{
  onSelect: (value: AutocompleteOption) => void;
  onClear?: () => void;
  autoFocus?: boolean;
}> = (props) => {
  const [options, setOptions] = useState([] as AutocompleteOption[]);

  return (
    <Autocomplete
      id="language-name-select"
      freeSolo
      options={options}
      onOpen={() => setOptions(getOptions(''))}
      filterOptions={(options) => options}
      getOptionLabel={(option) => option.englishName}
      onChange={(_, value) => {
        props.onSelect(value as AutocompleteOption);
      }}
      renderOption={(props, option) => (
        <MenuItem {...props}>
          <span data-cy="languages-create-autocomplete-suggested-option">
            {option.customRenderOption ||
              `${option.englishName} - ${option.originalName} - ${
                option.languageId
              } ${option.flags?.[0] || ''}`}
          </span>
        </MenuItem>
      )}
      renderInput={(params) => (
        <TextField
          data-cy="languages-create-autocomplete-field"
          {...params}
          onChange={(e) => {
            setTimeout(() => setOptions(getOptions(e.target.value)));
          }}
          label={<T>language_create_autocomplete_label</T>}
          margin="normal"
          variant="standard"
          required={true}
          InputProps={{
            autoFocus: props.autoFocus,
            ...params.InputProps,
            style: { paddingRight: 0 },
            endAdornment: props.onClear ? (
              <InputAdornment position="end">
                <IconButton size="small" onClick={props.onClear}>
                  <Clear />
                </IconButton>
              </InputAdornment>
            ) : undefined,
          }}
        />
      )}
    />
  );
};
