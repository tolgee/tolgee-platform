import { components } from 'tg.service/apiSchema.generated';
import React, { ComponentProps, useState } from 'react';
import Box from '@mui/material/Box';
import { useField, useFormikContext } from 'formik';
import { useTranslate } from '@tolgee/react';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { useDebounce } from 'use-debounce';
import { SelectItem } from 'tg.component/searchSelect/SelectItem';
import { InfiniteSearchSelect } from 'tg.component/searchSelect/InfiniteSearchSelect';
import { LanguageValue } from 'tg.component/languages/LanguageValue';
import { usePreferredOrganization } from 'tg.globalContext/helpers';

type OrganizationLanguageModel =
  components['schemas']['OrganizationLanguageModel'];
type SelectedLanguageModel = {
  tag: string;
  name: string;
  flagEmoji?: string;
};

type Props = {
  name: string;
  assignedProjectsName?: string;
  disabled?: boolean;
} & Omit<ComponentProps<typeof Box>, 'children'>;

export const GlossaryBaseLanguageSelect: React.VFC<Props> = ({
  name,
  assignedProjectsName,
  disabled,
  ...boxProps
}) => {
  const { preferredOrganization } = usePreferredOrganization();
  const context = useFormikContext();
  const { t } = useTranslate();
  const [field, meta] = useField(name);
  const value = field.value as SelectedLanguageModel | undefined;
  // Formik returns error as object - schema is incorrect...
  const error = (meta.error as any)?.tag;

  const [search, setSearch] = useState('');
  const [searchDebounced] = useDebounce(search, 500);

  // For filtering available languages by assigned projects
  const assignedProjects = assignedProjectsName
    ? context.getFieldProps(assignedProjectsName)?.value
    : undefined;
  const assignedProjectIds = assignedProjects?.map((p) => p.id);

  const query = {
    projectIds: assignedProjectIds,
    search: searchDebounced,
    size: 30,
  };
  const dataLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{organizationId}/base-languages',
    method: 'get',
    path: { organizationId: preferredOrganization!.id },
    query,
    options: {
      keepPreviousData: true,
      refetchOnMount: true,
      noGlobalLoading: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { id: preferredOrganization!.id },
            query: {
              ...query,
              page: lastPage.page!.number! + 1,
            },
          };
        } else {
          return null;
        }
      },
    },
  });

  const data = dataLoadable.data?.pages.flatMap(
    (p) => p._embedded?.languages ?? []
  );

  const handleFetchMore = () => {
    if (dataLoadable.hasNextPage && !dataLoadable.isFetching) {
      dataLoadable.fetchNextPage();
    }
  };

  const setValue = (v?: SelectedLanguageModel) =>
    context.setFieldValue(name, v);
  const setSelected = (item: OrganizationLanguageModel) => {
    const itemSelected: SelectedLanguageModel = {
      tag: item.tag,
      name: item.name,
      flagEmoji: item.flagEmoji,
    };
    setValue(itemSelected);
  };

  function renderItem(props: object, item: OrganizationLanguageModel) {
    const selected = value?.tag === item.tag;
    return (
      <SelectItem
        {...props}
        data-cy="glossary-base-language-select-item"
        selected={selected}
        label={<LanguageValue language={item} />}
        onClick={() => setSelected(item)}
      />
    );
  }

  function labelItem(item: SelectedLanguageModel) {
    return item.name + (item.flagEmoji ? ' ' + item.flagEmoji : '');
  }

  return (
    <Box data-cy="glossary-base-language-select" {...boxProps}>
      <InfiniteSearchSelect
        items={data}
        selected={value}
        queryResult={dataLoadable}
        itemKey={(item) => item.tag}
        search={search}
        onClearSelected={() => setValue(undefined)}
        onSearchChange={setSearch}
        onFetchMore={handleFetchMore}
        renderItem={renderItem}
        labelItem={labelItem}
        label={t('create_glossary_field_base_language')}
        error={meta.touched && error}
        searchPlaceholder={t('language_search_placeholder')}
        disabled={disabled}
      />
    </Box>
  );
};
