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
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';

type OrganizationLanguageModel =
  components['schemas']['OrganizationLanguageModel'];

function toSortedBaseFirst<T extends { base: boolean }>(list: T[]): T[] {
  return list.toSorted((a, b) => {
    if (a.base === b.base) return 0;
    return a.base ? -1 : 1;
  });
}

type Props = {
  disabled?: boolean;
  value: string[] | undefined;
  onValueChange: (value: string[]) => void;
} & Omit<ComponentProps<typeof Box>, 'children'>;

export const GlossaryViewLanguageSelect: React.VFC<Props> = ({
  disabled,
  value,
  onValueChange,
  ...boxProps
}) => {
  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();

  const { t } = useTranslate();

  const [search, setSearch] = useState('');
  const [searchDebounced] = useDebounce(search, 500);

  const glossaryLanguagesLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/languages',
    method: 'get',
    path: {
      organizationId: preferredOrganization!.id,
      glossaryId: glossary.id,
    },
  });

  const assignedProjectsIds = glossary.assignedProjects.map((p) => p.id);
  const query = {
    search: searchDebounced,
    projectIds:
      assignedProjectsIds === undefined || assignedProjectsIds.length === 0
        ? undefined
        : assignedProjectsIds,
    size: 30,
  };
  const organizationLanguagesLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{organizationId}/languages',
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

  const getGlossaryLanguageTags = () => {
    const languages =
      glossaryLanguagesLoadable.data?._embedded?.glossaryLanguageDtoList;
    return languages?.map((l) => l.tag) ?? [];
  };

  const getFirstPageOfOrganizationLanguageTags = () => {
    const firstPageOfOrganizationLanguages =
      organizationLanguagesLoadable.data?.pages[0]?._embedded?.languages;
    return firstPageOfOrganizationLanguages?.map((l) => l.tag) ?? [];
  };

  useEffect(() => {
    if (
      value === undefined &&
      glossaryLanguagesLoadable.data &&
      organizationLanguagesLoadable.data
    ) {
      // Calculate and set default value
      const glossaryLanguageTags = getGlossaryLanguageTags();
      const organizationLanguageTags = getFirstPageOfOrganizationLanguageTags();
      const uniqueOrganizationLanguageTags = organizationLanguageTags.filter(
        (l) => !glossaryLanguageTags.includes(l)
      );
      onValueChange([
        ...glossaryLanguageTags,
        ...uniqueOrganizationLanguageTags,
      ]);
    }
  }, [
    value,
    glossaryLanguagesLoadable.data,
    organizationLanguagesLoadable.data,
  ]);

  const organizationLanguages = useMemo(() => {
    const organizationLanguagesPages =
      organizationLanguagesLoadable.data?.pages ?? [];
    return organizationLanguagesPages.flatMap(
      (p) => p._embedded?.languages ?? []
    );
  }, [organizationLanguagesLoadable.data]);

  const languages: OrganizationLanguageModel[] = useMemo(() => {
    // List of all glossary and organization languages

    const glossaryLanguages =
      glossaryLanguagesLoadable.data?._embedded?.glossaryLanguageDtoList ?? [];
    const glossaryLanguagesBaseFirst = toSortedBaseFirst(glossaryLanguages);
    const glossaryLanguagesValue = glossaryLanguagesBaseFirst.map((l) => {
      const languageData = languageInfo[l.tag];
      return {
        base: l.base,
        tag: l.tag,
        flagEmoji: languageData?.flags?.[0] || '',
        originalName: languageData?.originalName || l.tag,
        name: languageData?.englishName || l.tag,
      };
    });

    const uniqueOrganizationLanguages = organizationLanguages.filter(
      (l) => !glossaryLanguages.some((pl) => pl.tag === l.tag)
    );
    const organizationLanguagesBaseFirst = toSortedBaseFirst(
      uniqueOrganizationLanguages
    );
    const organizationLanguagesValue = organizationLanguagesBaseFirst.map(
      (l) => ({ ...l, base: false })
    );

    return [...glossaryLanguagesValue, ...organizationLanguagesValue];
  }, [glossaryLanguagesLoadable.data, organizationLanguages]);

  const handleFetchMore = () => {
    if (
      organizationLanguagesLoadable.hasNextPage &&
      !organizationLanguagesLoadable.isFetching
    ) {
      organizationLanguagesLoadable.fetchNextPage();
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

  function renderItem(props: object, item: OrganizationLanguageModel) {
    const selected = value?.includes(item.tag) || false;
    return (
      <MultiselectItem
        {...props}
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
        items={languages}
        selected={value}
        queryResult={organizationLanguagesLoadable}
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
