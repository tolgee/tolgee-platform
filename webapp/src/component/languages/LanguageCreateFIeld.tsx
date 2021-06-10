import { FunctionComponent, ReactNode, useState } from 'react';
import { useFormikContext } from 'formik';
import { Alert, Autocomplete } from '@material-ui/lab';
import { TextField } from '../common/form/fields/TextField';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import { FlagSelector } from './FlagSelector';
import { isValidLanguageTag, suggest } from '@tginternal/language-util';
import { SuggestResult } from '@tginternal/language-util/lib/suggesting';
import { Add } from '@material-ui/icons';

type AutocompleteOption = Omit<SuggestResult, 'languageId'> & {
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

export const LanguageCreateField: FunctionComponent = () => {
  const [options, setOptions] = useState([] as AutocompleteOption[]);
  const { setFieldValue, values } = useFormikContext();
  const [tagValid, setTagValid] = useState(true);
  const [preferredEmojis, setPreferredEmojis] = useState([] as string[]);
  const [selected, setSelected] = useState(false);

  const onSelect = (value: SuggestResult) => {
    if (value) {
      setFieldValue('originalName', value.originalName);
      setFieldValue('tag', value.languageId);
      if (value?.flags?.[0]) {
        setFieldValue('flag', value.flags[0]);
      }
      setPreferredEmojis(value.flags);
      setSelected(true);
    }
  };

  const validateTag = () => {
    const tagValue = (values as any)['tag'];
    setTimeout(() => setTagValid(!tagValue || isValidLanguageTag(tagValue)));
  };

  return (
    <>
      <Autocomplete
        id="language-name-select"
        freeSolo
        options={options}
        onOpen={() => setOptions(getOptions(''))}
        filterOptions={(options) => options}
        getOptionLabel={(option) => option.englishName}
        onChange={(_, value) => {
          onSelect(value as SuggestResult);
        }}
        renderOption={(option) =>
          option.customRenderOption ||
          `${option.englishName} - ${option.originalName} - ${
            option.languageId
          } ${option.flags?.[0] || ''}`
        }
        renderInput={(params) => (
          <TextField
            {...params}
            onChange={(e) => {
              setTimeout(() => setOptions(getOptions(e.target.value)));
            }}
            name={'englishName'}
            label={<T>language_create_edit_english_name_label</T>}
            margin="normal"
            required={true}
          />
        )}
      />
      {selected && (
        <>
          <TextField
            label={<T>language_create_edit_original_name_label</T>}
            name="originalName"
            required={true}
          />
          <Box display="flex">
            <FlagSelector preferredEmojis={preferredEmojis} name="flag" />
            <Box flexGrow={1} ml={2}>
              <TextField
                fullWidth
                label={<T>language_create_edit_abbreviation</T>}
                name="tag"
                required={true}
                onValueChange={() => validateTag()}
              />
              {!tagValid && (
                <Box mb={4}>
                  <Alert color="warning">
                    <T>invalid_language_tag</T>
                  </Alert>
                </Box>
              )}
            </Box>
          </Box>
        </>
      )}
    </>
  );
};
