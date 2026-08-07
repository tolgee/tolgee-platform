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

const ALL_ORGANIZATIONS_OPTION = { kind: 'all' } as const;

type Option =
  | typeof ALL_ORGANIZATIONS_OPTION
  | ({ kind: 'organization' } & SelectableOrganization);

type Props = {
  value: SelectableOrganization | null;
  disabled?: boolean;
  allOptionVisible?: boolean;
  onChange: (organization: SelectableOrganization | null) => void;
  onSelectAll: () => void;
};

export const AppOrganizationSelect = ({
  value,
  disabled,
  allOptionVisible,
  onChange,
  onSelectAll,
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

  const organizationOptions: Option[] = (
    organizationsLoadable.data?._embedded?.organizations ?? []
  ).map((organization) => ({
    kind: 'organization',
    id: organization.id,
    name: organization.name,
  }));

  const selectedOption: Option | null = value
    ? { kind: 'organization', ...value }
    : null;

  // The selected organization has to stay among the options, otherwise the search
  // query narrowing the list would make Autocomplete drop the current value.
  const selectedMissing =
    value !== null &&
    !organizationOptions.some(
      (option) => option.kind === 'organization' && option.id === value.id
    );

  const options: Option[] = [
    ...(allOptionVisible ? [ALL_ORGANIZATIONS_OPTION] : []),
    ...(selectedMissing && selectedOption ? [selectedOption] : []),
    ...organizationOptions,
  ];

  const getLabel = (option: Option) =>
    option.kind === 'all'
      ? t('administration_apps_organization_option_all', 'All organizations')
      : t('administration_apps_organization_option', '{name} (id: {id})', {
          name: option.name,
          id: String(option.id),
        });

  return (
    <Autocomplete
      size="small"
      disabled={disabled}
      options={options}
      loading={organizationsLoadable.isFetching}
      getOptionLabel={getLabel}
      filterOptions={(items) => items}
      isOptionEqualToValue={(option, selected) =>
        option.kind === selected.kind &&
        (option.kind !== 'organization' ||
          selected.kind !== 'organization' ||
          option.id === selected.id)
      }
      value={selectedOption}
      onChange={(_, newValue) => {
        if (!newValue) {
          onChange(null);
          return;
        }
        if (newValue.kind === 'all') {
          onSelectAll();
          return;
        }
        onChange({ id: newValue.id, name: newValue.name });
      }}
      onInputChange={(_, newValue, reason) => {
        if (reason === 'input') {
          setSearch(newValue);
          return;
        }
        if (reason === 'clear') {
          setSearch('');
        }
      }}
      renderOption={(props, option) =>
        option.kind === 'all' ? (
          <li
            {...props}
            key="all"
            data-cy="administration-apps-organization-option-all"
          >
            {getLabel(option)}
          </li>
        ) : (
          <li
            {...props}
            key={option.id}
            data-cy="administration-apps-organization-option"
            data-cy-organization-id={option.id}
          >
            {getLabel(option)}
          </li>
        )
      }
      renderInput={(params) => (
        <TextField
          {...params}
          label={t(
            'administration_apps_organization_select_label',
            'Organization'
          )}
          placeholder={t(
            'administration_apps_organization_select_placeholder',
            'Search organizations…'
          )}
          data-cy="administration-apps-organization-select"
        />
      )}
      noOptionsText={t(
        'administration_apps_organization_select_empty',
        'No matching organizations'
      )}
    />
  );
};
