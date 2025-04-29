import { components } from 'tg.service/apiSchema.generated';
import React, { ComponentProps, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import { useTranslate } from '@tolgee/react';
import { useApiInfiniteQuery, useApiQuery } from 'tg.service/http/useQueryApi';
import { useDebounce } from 'use-debounce';
import { LanguageValue } from 'tg.component/languages/LanguageValue';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { InfiniteMultiSearchSelect } from 'tg.component/searchSelect/InfiniteMultiSearchSelect';
import { MultiselectItem } from 'tg.component/searchSelect/MultiselectItem';

type OrganizationLanguageModel =
  components['schemas']['OrganizationLanguageModel'];

type Props = {
  organizationId: number;
  glossaryId: number;
  disabled?: boolean;
  value: string[] | undefined;
  onValueChange: (value: string[]) => void;
} & Omit<ComponentProps<typeof Box>, 'children'>;

export const GlossaryViewLanguageSelect: React.VFC<Props> = ({
  organizationId,
  glossaryId,
  disabled,
  value,
  onValueChange,
  ...boxProps
}) => {
  const { t } = useTranslate();

  const [search, setSearch] = useState('');
  const [searchDebounced] = useDebounce(search, 500);

  const priorityDataLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/languages',
    method: 'get',
    path: { organizationId, glossaryId },
  });

  const query = {
    search: searchDebounced,
    size: 30,
  };
  const dataLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{organizationId}/languages',
    method: 'get',
    path: { organizationId: organizationId },
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
            path: { id: organizationId },
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

  useEffect(() => {
    if (value === undefined && priorityDataLoadable.data && dataLoadable.data) {
      const langs =
        priorityDataLoadable.data._embedded?.glossaryLanguageDtoList?.map(
          (l) => l.tag
        ) ?? [];
      const extraLangs =
        dataLoadable.data.pages[0]?._embedded?.languages?.map((l) => l.tag) ??
        [];
      extraLangs.forEach((l) => {
        if (!langs.includes(l)) {
          langs.push(l);
        }
      });
      onValueChange(langs);
    }
  }, [value, priorityDataLoadable.data, dataLoadable.data]);

  const dataExtra = useMemo(() => {
    return dataLoadable.data?.pages.flatMap(
      (p) => p._embedded?.languages ?? []
    );
  }, [dataLoadable.data]);

  const data: OrganizationLanguageModel[] = useMemo(() => {
    const priorityLangs =
      priorityDataLoadable.data?._embedded?.glossaryLanguageDtoList
        ?.toSorted((a, b) => {
          if (a.base === b.base) return 0;
          return a.base ? -1 : 1;
        })
        ?.map((l) => {
          const languageData = languageInfo[l.tag];
          return {
            base: l.base,
            tag: l.tag,
            flagEmoji: languageData?.flags?.[0] || '',
            originalName: languageData?.originalName || l.tag,
            name: languageData?.englishName || l.tag,
          };
        }) || [];
    const extraLangs =
      dataExtra
        ?.filter((l) => !priorityLangs.some((pl) => pl.tag === l.tag))
        ?.toSorted((a, b) => {
          if (a.base === b.base) return 0;
          return a.base ? -1 : 1;
        })
        ?.map((l) => ({ ...l, base: false })) || [];
    return [...priorityLangs, ...extraLangs];
  }, [priorityDataLoadable.data, dataExtra]);

  const handleFetchMore = () => {
    if (dataLoadable.hasNextPage && !dataLoadable.isFetching) {
      dataLoadable.fetchNextPage();
    }
  };

  const toggleSelected = (item: OrganizationLanguageModel) => {
    if (value === undefined) {
      onValueChange([item.tag]);
      return;
    }

    if (value.some((v) => v === item.tag)) {
      onValueChange(value.filter((v) => item.tag !== v));
      return;
    }
    onValueChange([item.tag, ...value]);
  };

  function renderItem(props: any, item: OrganizationLanguageModel) {
    const selected = value?.includes(item.tag) || false;
    return (
      <MultiselectItem
        disabled={item.base}
        selected={selected}
        label={<LanguageValue language={item} />}
        onClick={() => toggleSelected(item)}
      />
    );
  }

  function labelItem(item: string) {
    return item;
  }

  return (
    <Box {...boxProps}>
      <InfiniteMultiSearchSelect
        data-cy="glossary-view-language-select"
        items={data}
        selected={value}
        queryResult={dataLoadable}
        itemKey={(item) => item.tag}
        search={search}
        onClearSelected={() => onValueChange([])}
        onSearchChange={setSearch}
        onFetchMore={handleFetchMore}
        renderItem={renderItem}
        labelItem={labelItem}
        searchPlaceholder={t('language_search_placeholder')}
        disabled={disabled}
        minHeight={false}
      />
    </Box>
  );
};
