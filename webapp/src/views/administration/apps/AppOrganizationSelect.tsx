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
  excludedIds: number[];
  disabled?: boolean;
  allOptionVisible?: boolean;
  onSelect: (organization: SelectableOrganization) => void;
  onSelectAll: () => void;
};

export const AppOrganizationSelect = ({
  excludedIds,
  disabled,
  allOptionVisible,
  onSelect,
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

  const excluded = new Set(excludedIds);
  const organizationOptions: Option[] = (
    organizationsLoadable.data?._embedded?.organizations ?? []
  )
    .filter((organization) => !excluded.has(organization.id))
    .map((organization) => ({
      kind: 'organization',
      id: organization.id,
      name: organization.name,
    }));

  const options: Option[] = allOptionVisible
    ? [ALL_ORGANIZATIONS_OPTION, ...organizationOptions]
    : organizationOptions;

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
      value={null}
      onChange={(_, newValue) => {
        if (!newValue) {
          return;
        }
        if (newValue.kind === 'all') {
          onSelectAll();
        } else {
          onSelect({ id: newValue.id, name: newValue.name });
        }
        setSearch('');
      }}
      inputValue={search}
      onInputChange={(_, value) => setSearch(value)}
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
