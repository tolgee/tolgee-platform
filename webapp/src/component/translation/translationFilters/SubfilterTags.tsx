import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Menu } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { InfiniteSearchSelectContent } from 'tg.component/searchSelect/InfiniteSearchSelectContent';
import { FilterItem } from './FilterItem';
import {
  FiltersInternal,
  type FilterActions,
} from 'tg.views/projects/translations/context/services/useTranslationFilterService';

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
};

export const SubfilterTags = ({ value, actions, projectId }: Props) => {
  const tagsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tags',
    method: 'get',
    path: {
      projectId,
    },
    query: {},
  });

  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  const data = [
    { id: -1, name: '' },
    ...(tagsLoadable.data?._embedded?.tags || []),
  ];

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
          }}
        >
          <InfiniteSearchSelectContent
            open={true}
            items={data}
            maxWidth={400}
            displaySearch={false}
            compareFunction={(prompt, item) =>
              item.name.toLowerCase().startsWith(prompt.toLocaleLowerCase())
            }
            renderOption={(props, item) => (
              <FilterItem
                {...props}
                label={item.name || `<${t('subfilter_without_tags')}>`}
                selected={Boolean(value.filterTag?.includes(item.name))}
                excluded={Boolean(value.filterNoTag?.includes(item.name))}
                onClick={() => {
                  if (value.filterTag?.includes(item.name)) {
                    actions.removeFilter('filterTag', item.name);
                  } else if (value.filterNoTag?.includes(item.name)) {
                    actions.removeFilter('filterNoTag', item.name);
                  } else {
                    actions.addFilter('filterTag', item.name);
                  }
                }}
                onExclude={() => {
                  if (value.filterNoTag?.includes(item.name)) {
                    actions.removeFilter('filterNoTag', item.name);
                  } else {
                    actions.addFilter('filterNoTag', item.name);
                  }
                }}
              />
            )}
            getOptionLabel={(o) => o.name}
          />
        </Menu>
      )}
    </>
  );
};

export function getTagFiltersLength(value: FiltersInternal) {
  return (value.filterTag?.length ?? 0) + (value.filterNoTag?.length ?? 0);
}
