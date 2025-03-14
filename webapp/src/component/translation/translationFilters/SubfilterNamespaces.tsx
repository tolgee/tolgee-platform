import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Divider, Menu } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { InfiniteSearchSelectContent } from 'tg.component/searchSelect/InfiniteSearchSelectContent';
import {
  FiltersInternal,
  type FilterActions,
} from 'tg.views/projects/translations/context/services/useTranslationFilterService';
import { FilterItem } from './FilterItem';
import { useDebounce } from 'use-debounce';

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
  const tagsLoadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/namespaces',
    method: 'get',
    path: {
      projectId,
    },
    query,
    options: {
      keepPreviousData: true,
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

  const data = tagsLoadable.data?.pages.flatMap(
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
    if (tagsLoadable.hasNextPage && tagsLoadable.isFetching) {
      tagsLoadable.fetchNextPage();
    }
  };

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_namespaces')}
        onClick={() => setOpen(true)}
        selected={Boolean(getTagFiltersLength(value))}
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
          {Boolean(totalItems) && (
            <>
              <InfiniteSearchSelectContent
                open={true}
                items={data}
                maxWidth={400}
                onSearch={setSearch}
                search={search}
                displaySearch={(totalItems ?? 0) > 10}
                renderOption={(props, item) => (
                  <FilterItem
                    {...props}
                    label={item.name}
                    selected={Boolean(
                      value.filterNamespace?.includes(item.name)
                    )}
                    excluded={Boolean(
                      value.filterNoNamespace?.includes(item.name)
                    )}
                    onClick={() => handleToggleNamespace(item.name)}
                    onExclude={() => handleExcludeNamespace(item.name)}
                  />
                )}
                getOptionLabel={(o) => o.name}
                ListboxProps={{ style: { maxHeight: 400, overflow: 'auto' } }}
                searchPlaceholder={t(
                  'translations_filters_tags_search_placeholder'
                )}
                onGetMoreData={handleFetchMore}
              />
              <Divider />
            </>
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

export function getTagFiltersLength(value: FiltersInternal) {
  return (
    (value.filterNamespace?.length ?? 0) +
    (value.filterNoNamespace?.length ?? 0)
  );
}
