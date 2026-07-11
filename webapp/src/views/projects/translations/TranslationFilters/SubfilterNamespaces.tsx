import React, { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Divider, Menu } from '@mui/material';
import { useDebounce } from 'use-debounce';

import { SmoothProgress } from 'tg.component/SmoothProgress';
import { SubmenuItem } from 'tg.component/SubmenuItem';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { InfiniteSearchSelectContent } from 'tg.component/searchSelect/InfiniteSearchSelectContent';
import { FiltersInternal, FilterActions } from './tools';
import { components } from 'tg.service/apiSchema.generated';
import { FilterItem } from './FilterItem';

type NamespaceModel = components['schemas']['NamespaceModel'];

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
};

export const SubfilterNamespaces = ({ value, actions, projectId }: Props) => {
  const [search, setSearch] = useState('');
  const [totalItems, setTotalItems] = useState<number | undefined>(undefined);
  const [searchDebounced] = useDebounce(search, 500);
  const query = {
    search: searchDebounced,
    size: 30,
  };
  const dataLoadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/namespaces',
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
    (p) => p._embedded?.namespaces ?? []
  );

  const handleToggleNamespace = (name: string) => {
    if (value.filterNamespace?.includes(name)) {
      actions.removeFilter('filterNamespace', name);
    } else if (value.filterNoNamespace?.includes(name)) {
      actions.removeFilter('filterNoNamespace', name);
    } else {
      actions.addFilter('filterNamespace', name);
    }
  };

  const handleExcludeNamespace = (name: string) => {
    if (value.filterNoNamespace?.includes(name)) {
      actions.removeFilter('filterNoNamespace', name);
    } else {
      actions.addFilter('filterNoNamespace', name);
    }
  };

  const handleFetchMore = () => {
    if (dataLoadable.hasNextPage && !dataLoadable.isFetching) {
      dataLoadable.fetchNextPage();
    }
  };

  function renderItem(props: any, item: NamespaceModel) {
    return (
      <FilterItem
        label={item.name}
        selected={Boolean(value.filterNamespace?.includes(item.name))}
        excluded={Boolean(value.filterNoNamespace?.includes(item.name))}
        onClick={() => handleToggleNamespace(item.name)}
        onExclude={() => handleExcludeNamespace(item.name)}
      />
    );
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_namespaces')}
        onClick={() => setOpen(true)}
        selected={Boolean(getNamespaceFiltersLength(value))}
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
                itemKey={(item) => item.id}
                maxWidth={400}
                onSearch={setSearch}
                search={search}
                displaySearch={(totalItems ?? 0) > 10}
                renderOption={renderItem}
                ListboxProps={{ style: { maxHeight: 400, overflow: 'auto' } }}
                searchPlaceholder={t(
                  'translations_filters_namespaces_search_placeholder'
                )}
                onGetMoreData={handleFetchMore}
              />
              <Divider sx={{ my: 1 }} />
            </Box>
          )}
          <FilterItem
            label={t('translations_filters_namespaces_without_namespace')}
            selected={Boolean(value.filterNamespace?.includes(''))}
            excluded={Boolean(value.filterNoNamespace?.includes(''))}
            onClick={() => handleToggleNamespace('')}
            onExclude={() => handleExcludeNamespace('')}
          />
        </Menu>
      )}
    </>
  );
};

export function getNamespaceFiltersLength(value: FiltersInternal) {
  return (
    (value.filterNamespace?.length ?? 0) +
    (value.filterNoNamespace?.length ?? 0)
  );
}

export function getNamespaceFiltersName(value: FiltersInternal) {
  if (value.filterNamespace?.length) {
    return (
      value.filterNamespace[0] || (
        <T keyName="translations_filters_namespaces_without_namespace" />
      )
    );
  }

  if (value.filterNoNamespace?.length) {
    return (
      <Box sx={{ display: 'inline', textDecoration: 'line-through' }}>
        {value.filterNoNamespace[0] || (
          <T keyName="translations_filters_namespaces_without_namespace" />
        )}
      </Box>
    );
  }
}
