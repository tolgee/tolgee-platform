import { Autocomplete, MenuItem, TextField } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useEffect, useMemo, useState } from 'react';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

type OptionType = { name: string; value: string; isNew: boolean };

type Props = {
  value: string | undefined;
  onChange: (value: string | undefined) => void;
};

export const NamespaceSelector: React.FC<Props> = ({ value, onChange }) => {
  const project = useProject();

  const namespacesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/namespaces',
    method: 'get',
    path: { projectId: project.id },
    query: {},
    fetchOptions: {
      disableNotFoundHandling: true,
    },
  });

  const currentNamespace = value || '';

  const existingOptions = useMemo(() => {
    const existing = namespacesLoadable?.data?._embedded?.namespaces?.map(
      (ns) => ns.name
    );
    if (!existing) {
      return [];
    }
    return ['', ...existing].map((o) => ({
      value: o,
      name: o,
      isNew: false,
    }));
  }, [namespacesLoadable.data]);

  const [options, setOptions] = useState(existingOptions);

  useEffect(() => {
    setOptions(existingOptions);
  }, [namespacesLoadable.isFetched]);

  const t = useTranslate();

  const inputBlurredValue =
    currentNamespace === ''
      ? t({ key: 'namespace_select_default' })
      : currentNamespace;

  const [inputFocus, setInputFocus] = useState(false);

  const [inputValue, setInputValue] = useState('');

  return (
    <Autocomplete
      id="namespace-autocomplete"
      onChange={(_, option) => {
        onChange(typeof option === 'string' ? option : option?.value);
      }}
      freeSolo
      onFocus={() => setInputFocus(true)}
      onBlur={() => setInputFocus(false)}
      inputValue={!inputFocus ? inputBlurredValue : inputValue}
      onInputChange={(_, value) => {
        if (options.findIndex((o) => o.name === value) < 0) {
          setOptions([
            { name: `Add "${value}"`, value, isNew: true },
            ...existingOptions,
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
          <span data-cy="import-namespace-select-option">
            {option.name === '' ? t('namespace_select_default') : option.name}
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
          data-cy="import-namespaces-text-field"
          size={'small'}
          variant="outlined"
          onSubmit={() => {}}
        />
      )}
    />
  );
};
