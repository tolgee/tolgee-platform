import { Autocomplete, MenuItem, TextField } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useEffect, useMemo, useState } from 'react';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

type OptionType = { name: string; value: string; isNew: boolean };

type Props = {
  value: string | undefined;
  onChange: (value: string | undefined) => void;
  namespaceData?: string[];
};

export const NamespaceSelector: React.FC<Props> = ({
  value,
  onChange,
  namespaceData,
}) => {
  const project = useProject();

  const namespacesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/namespaces',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 1000,
    },
    fetchOptions: {
      disableNotFoundHandling: true,
    },
    options: {
      enabled: !namespaceData,
    },
  });

  const currentNamespace = value || '';

  const existingOptions = useMemo(() => {
    const existing =
      namespaceData ||
      namespacesLoadable?.data?._embedded?.namespaces?.map((ns) => ns.name) ||
      [];

    return ['', ...existing].map((o) => ({
      value: o,
      name: o,
      isNew: false,
    }));
  }, [namespacesLoadable.data, namespaceData]);

  const [options, setOptions] = useState(existingOptions);

  useEffect(() => {
    setOptions(existingOptions);
  }, [namespaceData]);

  const t = useTranslate();

  const inputBlurredValue =
    currentNamespace === ''
      ? t({ key: 'namespace_select_default' })
      : currentNamespace;

  const [inputFocus, setInputFocus] = useState(false);

  const [inputValue, setInputValue] = useState('');

  const handleBlur = () => {
    setInputFocus(false);
    setInputValue('');
  };

  const handleFocus = () => {
    setInputFocus(true);
    setInputValue('');
  };

  return (
    <Autocomplete
      id="namespace-autocomplete"
      data-cy="namespaces-select"
      onChange={(_, option) => {
        onChange(typeof option === 'string' ? option : option?.value);
      }}
      onFocus={handleFocus}
      onBlur={handleBlur}
      inputValue={!inputFocus ? inputBlurredValue : inputValue}
      onInputChange={(_, value) => {
        if (options.findIndex((o) => o.name === value) < 0 && value) {
          setOptions([
            ...existingOptions,
            { name: `Add "${value}"`, value, isNew: true },
          ]);
        } else {
          setOptions(existingOptions);
        }
        setInputValue(value);
      }}
      getOptionLabel={(o) => o.value || ''}
      blurOnSelect
      disableClearable
      options={options}
      renderOption={(props, option: OptionType) => (
        <MenuItem {...props}>
          <span data-cy="namespaces-select-option">
            {option.name === '' ? t('namespace_select_default') : option.name}
          </span>
        </MenuItem>
      )}
      renderInput={(params) => (
        <TextField
          {...params}
          data-cy="namespaces-select-text-field"
          size={'small'}
          variant="outlined"
          onSubmit={() => {}}
        />
      )}
    />
  );
};
