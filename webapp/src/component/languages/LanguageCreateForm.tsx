import React, { FunctionComponent, useEffect, useState } from 'react';
import { StandardForm } from '../common/form/StandardForm';
import { Validation } from '../../constants/GlobalValidationSchema';
import { TextField } from '../common/form/fields/TextField';
import { T } from '@tolgee/react';
import { LanguageDTO } from '../../service/response.types';
import { container } from 'tsyringe';
import { LanguageActions } from '../../store/languages/LanguageActions';
import { useProject } from '../../hooks/useProject';
import { Alert, Autocomplete } from '@material-ui/lab';
import { isValidLanguageTag, suggest } from '@tginternal/language-util';
import { useFormikContext } from 'formik';
import { SuggestResult } from '@tginternal/language-util/lib/suggesting';
import { Box } from '@material-ui/core';
import { FlagSelector } from './FlagSelector';

const actions = container.resolve(LanguageActions);
export const LanguageCreateForm: FunctionComponent<{
  onCancel: () => void;
  onCreated?: (language: LanguageDTO) => void;
}> = (props) => {
  const createLoadable = actions.useSelector((s) => s.loadables.create);
  const project = useProject();
  const [submitted, setSubmitted] = useState(false);

  const onSubmit = (values) => {
    setSubmitted(true);
    const dto: LanguageDTO = {
      ...values,
    };
    actions.loadableActions.create.dispatch(project.id, dto);
  };

  useEffect(() => {
    if (createLoadable.loaded && submitted) {
      props.onCreated && props.onCreated(createLoadable.data!);
      setSubmitted(false);
    }
  }, [createLoadable.loading]);

  return (
    <StandardForm
      initialValues={{ englishName: '', originalName: '', tag: '' }}
      onSubmit={onSubmit}
      onCancel={props.onCancel}
      saveActionLoadable={createLoadable}
      validationSchema={Validation.LANGUAGE}
    >
      <Inputs />
    </StandardForm>
  );
};

const Inputs: FunctionComponent = () => {
  const [options, setOptions] = useState([] as SuggestResult[]);
  const { setFieldValue, values } = useFormikContext();
  const [tagValid, setTagValid] = useState(true);
  const [preferredEmojis, setPreferredEmojis] = useState([] as string[]);

  const onSelect = (value: SuggestResult) => {
    if (value) {
      setFieldValue('originalName', value.originalName);
      setFieldValue('tag', value.languageId);
      if (value?.flags?.[0]) {
        setFieldValue('flag', value.flags[0]);
      }
      setPreferredEmojis(value.flags);
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
        onOpen={() => setOptions(suggest(''))}
        filterOptions={(options) => options}
        getOptionLabel={(option) => option.englishName}
        onChange={(_, value) => {
          onSelect(value as SuggestResult);
        }}
        renderOption={(option) =>
          option.englishName +
          ' - ' +
          option.originalName +
          ' - ' +
          option.languageId +
          ' ' +
          (option.flags?.[0] || '')
        }
        renderInput={(params) => (
          <TextField
            {...params}
            onChange={(e) => {
              setTimeout(() => setOptions(suggest(e.target.value)));
            }}
            name={'englishName'}
            label={<T>language_create_edit_english_name_label</T>}
            margin="normal"
            required={true}
          />
        )}
      />
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
  );
};
