import { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Autocomplete, TextField } from '@mui/material';
import { useDebounce } from 'use-debounce';

import { useApiQuery } from 'tg.service/http/useQueryApi';

const SEARCH_DEBOUNCE_MS = 300;

export type SelectableOrganization = {
  id: number;
  name: string;
};

type Props = {
  excludedIds: number[];
  disabled?: boolean;
  onSelect: (organization: SelectableOrganization) => void;
};

export const AppOrganizationSelect = ({
  excludedIds,
  disabled,
  onSelect,
}: Props) => {
  const { t } = useTranslate();
  const [search, setSearch] = useState('');
  const [searchDebounced] = useDebounce(search, SEARCH_DEBOUNCE_MS);

  const organizationsLoadable = useApiQuery({
    url: '/v2/administration/organizations',
    method: 'get',
    query: {
      search: searchDebounced || undefined,
      size: 20,
      sort: ['name,asc'],
    },
    options: {
      keepPreviousData: true,
      noGlobalLoading: true,
    },
  });

  const excluded = new Set(excludedIds);
  const options = (
    organizationsLoadable.data?._embedded?.organizations ?? []
  ).filter((organization) => !excluded.has(organization.id));

  const getLabel = (organization: SelectableOrganization) =>
    t('administration_apps_organization_option', '{name} (id: {id})', {
      name: organization.name,
      id: String(organization.id),
    });

  return (
    <Autocomplete
      size="small"
      disabled={disabled}
      options={options}
      loading={organizationsLoadable.isFetching}
      getOptionLabel={getLabel}
      filterOptions={(items) => items}
      value={null}
      onChange={(_, newValue) => {
        if (newValue) {
          onSelect({ id: newValue.id, name: newValue.name });
          setSearch('');
        }
      }}
      inputValue={search}
      onInputChange={(_, value) => setSearch(value)}
      renderOption={(props, option) => (
        <li
          {...props}
          key={option.id}
          data-cy="administration-apps-organization-option"
          data-cy-organization-id={option.id}
        >
          {getLabel(option)}
        </li>
      )}
      renderInput={(params) => (
        <TextField
          {...params}
          placeholder={t(
            'administration_apps_organization_select_placeholder',
            'Add organization…'
          )}
          data-cy="administration-apps-organization-select"
        />
      )}
      noOptionsText={t(
        'administration_apps_organization_select_empty',
        'No matching organizations'
      )}
      blurOnSelect
      clearOnBlur
    />
  );
};
