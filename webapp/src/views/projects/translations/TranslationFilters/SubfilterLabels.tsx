import React, { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem } from '@mui/material';
import { useDebounce } from 'use-debounce';

import { components } from 'tg.service/apiSchema.generated';
import { SubmenuItem } from 'tg.component/SubmenuItem';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { InfiniteSearchSelectContent } from 'tg.component/searchSelect/InfiniteSearchSelectContent';

import { FiltersInternal, FilterActions, LanguageModel } from './tools';
import { FilterItem } from './FilterItem';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { TranslationLabel } from 'tg.component/TranslationLabel';
import { CompactListSubheader } from 'tg.component/ListComponents';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

type LabelModel = components['schemas']['LabelModel'];

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
  selectedLanguages: LanguageModel[];
};

export const SubfilterLabels = ({
  value,
  actions,
  projectId,
  selectedLanguages,
}: Props) => {
  const [search, setSearch] = useState('');
  const [totalItems, setTotalItems] = useState<number | undefined>(undefined);
  const [searchDebounced] = useDebounce(search, 500);

  const [expanded, setExpanded] = useState(
    value.filterTranslationLanguage !== undefined
  );

  function toggleFilterLanguage(
    newValue: FiltersInternal['filterTranslationLanguage']
  ) {
    actions.setFilters({
      ...value,
      filterTranslationLanguage:
        newValue === value.filterTranslationLanguage ? undefined : newValue,
    });
  }

  const query = {
    search: searchDebounced,
    size: 30,
  };
  const dataLoadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/labels',
    method: 'get',
    path: {
      projectId,
    },
    query,
    options: {
      keepPreviousData: true,
      refetchOnMount: true,
      noGlobalLoading: true,
      onSuccess(data) {
        if (
          totalItems === undefined &&
          data.pages[0]?.page?.totalElements !== undefined
        ) {
          setTotalItems(data.pages[0].page.totalElements);
        }
      },
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: {
              projectId,
            },
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

  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  const data = dataLoadable.data?.pages.flatMap(
    (p) => p._embedded?.labels ?? []
  );

  const handleToggleLabel = (id: string) => {
    if (value.filterLabel?.includes(id)) {
      actions.removeFilter('filterLabel', id);
    } else {
      actions.addFilter('filterLabel', id);
    }
  };

  const handleFetchMore = async () => {
    if (dataLoadable.hasNextPage && !dataLoadable.isFetching) {
      await dataLoadable.fetchNextPage();
    }
  };

  function renderItem(props: any, item: LabelModel) {
    return (
      <FilterItem
        label={item.name}
        selected={Boolean(value.filterLabel?.includes(item.id.toString()))}
        onClick={() => handleToggleLabel(item.id.toString())}
      />
    );
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_labels')}
        onClick={() => setOpen(true)}
        selected={Boolean(getLabelFiltersLength(value))}
        open={open}
      />
      {open && (
        <Menu
          open={open}
          anchorEl={anchorEl.current!}
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
          onClose={() => {
            setOpen(false);
            setSearch('');
          }}
        >
          <SmoothProgress
            loading={Boolean(
              dataLoadable.isFetching &&
                (searchDebounced || totalItems === undefined)
            )}
            sx={{ position: 'absolute', top: 0, left: 0, right: 0 }}
          />
          {Boolean(totalItems) && (
            <Box display="grid">
              <InfiniteSearchSelectContent
                open={true}
                items={data}
                maxWidth={400}
                onSearch={setSearch}
                search={search}
                displaySearch={(totalItems ?? 0) > 10}
                renderOption={renderItem}
                getOptionLabel={(o) => o.name}
                ListboxProps={{
                  style: { maxHeight: 400, overflow: 'auto' },
                }}
                searchPlaceholder={t(
                  'translations_filters_labels_search_placeholder'
                )}
                onGetMoreData={handleFetchMore}
              />
              <CompactListSubheader>
                <Box display="flex" justifyContent="space-between">
                  <Box>{t('translations_filter_languages_select_title')}</Box>
                </Box>
              </CompactListSubheader>
              <FilterItem
                data-cy="translations-filter-apply-no-base"
                label={t('translations_filter_languages_no_base')}
                selected={value.filterTranslationLanguage === undefined}
                onClick={() => toggleFilterLanguage(undefined)}
                exclusive
              />
              {expanded && (
                <>
                  <FilterItem
                    data-cy="translations-filter-apply-for-all"
                    label={t('translations_filter_languages_all')}
                    selected={value.filterTranslationLanguage === true}
                    onClick={() => toggleFilterLanguage(true)}
                    exclusive
                  />
                  {selectedLanguages?.map((lang) => {
                    return (
                      <FilterItem
                        data-cy="translations-filter-apply-for-language"
                        key={lang.id}
                        label={lang.name}
                        selected={value.filterTranslationLanguage === lang.tag}
                        onClick={() => toggleFilterLanguage(lang.tag)}
                        exclusive
                      />
                    );
                  })}
                </>
              )}
              <MenuItem
                data-cy="translations-filter-apply-for-expand"
                role="button"
                onClick={() => setExpanded((value) => !value)}
                sx={{
                  display: 'flex',
                  justifyContent: 'center',
                }}
              >
                {expanded ? <ChevronUp /> : <ChevronDown />}
              </MenuItem>
            </Box>
          )}
        </Menu>
      )}
    </>
  );
};

export function getLabelFiltersLength(value: FiltersInternal) {
  return value.filterLabel?.length ?? 0;
}

export function getLabelFiltersName(value: FiltersInternal) {
  if (value.filterLabel?.length) {
    const labelId = value.filterLabel[0];
    return labelId ? (
      <TranslationLabel color={'FF0000'}>{labelId}</TranslationLabel>
    ) : (
      <T keyName="translations_filters_labels_without_labels" />
    );
  }
}
