import React, { FC, ReactNode, useMemo, useState } from 'react';
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
        <T keyName="language_field_autocomplete_label_new_language" />
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
  existingLanguages: string[];
}> = (props) => {
  const [options, setOptions] = useState([] as AutocompleteOption[]);
  const [search, setSearch] = useState('');
  const existingLanguages = useMemo(
    () => new Set(props.existingLanguages),
    [props.existingLanguages]
  );

  return (
    <Autocomplete
      id="language-name-select"
      options={options}
      onOpen={() => setOptions(getOptions(''))}
      filterOptions={(options) => options}
      getOptionLabel={(option) => option.englishName}
      inputValue={search}
      onInputChange={(_, value) => setSearch(value)}
      value={null}
      onChange={(_, value) => {
        props.onSelect(value as AutocompleteOption);
        setSearch('');
      }}
      renderOption={(props, option) => {
        const itemContent = (
          <span data-cy="languages-create-autocomplete-suggested-option">
            {option.customRenderOption ||
              `${option.englishName} - ${option.originalName} - ${
                option.languageId
              } ${option.flags?.[0] || ''}`}
          </span>
        );
        return existingLanguages.has(option.languageId) ? (
          <MenuItem key={option.languageId + ':disabled'} disabled={true}>
            {itemContent}
          </MenuItem>
        ) : (
          <MenuItem key={option.languageId} {...props}>
            {itemContent}
          </MenuItem>
        );
      }}
      renderInput={(params) => (
        <TextField
          data-cy="languages-create-autocomplete-field"
          {...params}
          onChange={(e) => {
            setTimeout(() => setOptions(getOptions(e.target.value)));
          }}
          label={<T keyName="language_create_autocomplete_label" />}
          margin="normal"
          variant="outlined"
          size="small"
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
