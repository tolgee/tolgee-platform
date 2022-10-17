import { Autocomplete, MenuItem, TextField } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import React, { useState } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useImportDataHelper } from '../hooks/useImportDataHelper';

type OptionType = { name: string; isNew: boolean };

export const NamespaceSelector = ({
  row,
}: {
  row: components['schemas']['ImportLanguageModel'];
}) => {
  const project = useProject();

  const dataHelper = useImportDataHelper();

  const mutation = useApiMutation({
    url: '/v2/projects/{projectId}/import/result/files/{fileId}/select-namespace',
    method: 'put',
    options: {
      onSuccess() {
        dataHelper.refetchData();
      },
    },
  });

  const currentNamespace = row.namespace || '';

  const existingOptions = ['', 'homepage'];

  const getExistingOptions = () =>
    existingOptions.map((o) => ({
      name: o,
      isNew: false,
    }));

  const [options, setOptions] = useState(getExistingOptions);

  const applyNamespace = (namespace: string) => {
    mutation.mutate({
      path: { projectId: project.id, fileId: row.importFileId },
      content: {
        'application/json': {
          namespace: namespace,
        },
      },
    });
  };

  const t = useTranslate();

  const inputBlurredValue =
    currentNamespace === ''
      ? t({ key: 'import_namespace_select_default', noWrap: true })
      : currentNamespace;

  const [inputFocus, setInputFocus] = useState(false);

  const [inputValue, setInputValue] = useState(inputBlurredValue);

  return (
    <Autocomplete
      loading={mutation.isLoading}
      sx={{ width: '150px' }}
      id="import-namespace-autocomplete"
      onChange={(_, option) => {
        applyNamespace(typeof option === 'string' ? option : option?.name);
      }}
      freeSolo
      onFocus={() => setInputFocus(true)}
      onBlur={() => setInputFocus(false)}
      inputValue={!inputFocus ? inputBlurredValue : inputValue}
      onInputChange={(_, value) => {
        if (options.findIndex((o) => o.name === value) < 0) {
          setOptions([{ name: value, isNew: true }, ...getExistingOptions()]);
        } else {
          setOptions(getExistingOptions());
        }
        setInputValue(value);
      }}
      getOptionLabel={(o) => o.name || ''}
      blurOnSelect
      disableClearable
      options={options}
      renderOption={(props, option: OptionType) => (
        <MenuItem {...props}>
          <span data-cy="import-namespace-select-option">
            {option.name === ''
              ? t('import_namespace_select_default')
              : option.name}
          </span>
        </MenuItem>
      )}
      componentsProps={{
        paper: {
          sx: {
            width: '300px',
          },
        },
      }}
      renderInput={(params) => (
        <TextField
          {...params}
          size={'small'}
          variant="standard"
          label={<T>import_namespace_autocomplete_label</T>}
          onSubmit={() => {}}
        />
      )}
    />
  );
};
