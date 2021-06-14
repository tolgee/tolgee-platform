import { Autocomplete } from '@material-ui/lab';
import { TextField } from '@material-ui/core';
import { T } from '@tolgee/react';
import React, { FC, ReactNode, useState } from 'react';
import { suggest } from '@tginternal/language-util';
import { Add } from '@material-ui/icons';
import { SuggestResult } from '@tginternal/language-util/lib/suggesting';

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
    flags: ['🏁'],
    languageId: 'custom',
    originalName: '',
    englishName: input,
    customRenderOption: (
      <>
        <Add />
        <T>language_field_autocomplete_label_new_language</T>
        &nbsp;🏁
      </>
    ),
  };
  return [newLang, ...suggest(input)];
};

export const LanguageAutocomplete: FC<{
  onSelect: (value: AutocompleteOption) => void;
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
      renderOption={(option) => (
        <span data-cy="languages-create-autocomplete-suggested-option">
          {option.customRenderOption ||
            `${option.englishName} - ${option.originalName} - ${
              option.languageId
            } ${option.flags?.[0] || ''}`}
        </span>
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
          required={true}
        />
      )}
    />
  );
};
