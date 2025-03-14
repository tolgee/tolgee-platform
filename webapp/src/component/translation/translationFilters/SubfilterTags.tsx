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

export const SubfilterTags = ({ value, actions, projectId }: Props) => {
  const [search, setSearch] = useState('');
  const [totalTags, setTotalTags] = useState<number | undefined>(undefined);
  const [searchDebounced] = useDebounce(search, 500);
  const query = {
    search: searchDebounced,
    size: 30,
  };
  const tagsLoadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/tags',
    method: 'get',
    path: {
      projectId,
    },
    query,
    options: {
      keepPreviousData: true,
      onSuccess(data) {
        if (
          totalTags === undefined &&
          data.pages[0]?.page?.totalElements !== undefined
        ) {
          setTotalTags(data.pages[0].page.totalElements);
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

  const data = tagsLoadable.data?.pages.flatMap((p) => p._embedded?.tags ?? []);

  const handleToggleTag = (name: string) => {
    if (value.filterTag?.includes(name)) {
      actions.removeFilter('filterTag', name);
    } else if (value.filterNoTag?.includes(name)) {
      actions.removeFilter('filterNoTag', name);
    } else {
      actions.addFilter('filterTag', name);
    }
  };

  const handleExcludeTag = (name: string) => {
    if (value.filterNoTag?.includes(name)) {
      actions.removeFilter('filterNoTag', name);
    } else {
      actions.addFilter('filterNoTag', name);
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
        label={t('translations_filters_heading_tags')}
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
          {Boolean(totalTags) && (
            <>
              <InfiniteSearchSelectContent
                open={true}
                items={data}
                maxWidth={400}
                onSearch={setSearch}
                search={search}
                displaySearch={(totalTags ?? 0) > 10}
                renderOption={(props, item) => (
                  <FilterItem
                    {...props}
                    label={item.name}
                    selected={Boolean(value.filterTag?.includes(item.name))}
                    excluded={Boolean(value.filterNoTag?.includes(item.name))}
                    onClick={() => handleToggleTag(item.name)}
                    onExclude={() => handleExcludeTag(item.name)}
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
            label={t('translations_filters_tags_without_tags')}
            selected={Boolean(value.filterTag?.includes(''))}
            excluded={Boolean(value.filterNoTag?.includes(''))}
            onClick={() => handleToggleTag('')}
            onExclude={() => handleExcludeTag('')}
          />
        </Menu>
      )}
    </>
  );
};

export function getTagFiltersLength(value: FiltersInternal) {
  return (value.filterTag?.length ?? 0) + (value.filterNoTag?.length ?? 0);
}
