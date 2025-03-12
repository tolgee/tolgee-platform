import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Menu } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { InfiniteSearchSelectContent } from 'tg.component/searchSelect/InfiniteSearchSelectContent';
import { FilterItem } from './FilterItem';
import {
  type FiltersInternal,
  type FilterActions,
} from 'tg.views/projects/translations/context/services/useTranslationFilterService';

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
};

export const SubfilterNamespaces = ({ value, actions, projectId }: Props) => {
  const tagsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/used-namespaces',
    method: 'get',
    path: {
      projectId,
    },
  });

  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  const data = tagsLoadable.data?._embedded?.namespaces || [];

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_namespaces')}
        onClick={() => setOpen(true)}
        selected={Boolean(getNamespacesFiltersLength(value))}
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
            items={data.map((i) => ({
              id: i.id || -1,
              name: i.name || '',
            }))}
            maxWidth={400}
            displaySearch={false}
            compareFunction={(prompt, item) =>
              item.name.toLowerCase().startsWith(prompt.toLocaleLowerCase())
            }
            renderOption={(props, item) => (
              <FilterItem
                {...props}
                label={item.name || t('namespace_default')}
                selected={Boolean(value.filterNamespace?.includes(item.name))}
                excluded={Boolean(value.filterNoNamespace?.includes(item.name))}
                onClick={() => {
                  if (value.filterNamespace?.includes(item.name)) {
                    actions.removeFilter('filterNamespace', item.name);
                  } else if (value.filterNoNamespace?.includes(item.name)) {
                    actions.removeFilter('filterNoNamespace', item.name);
                  } else {
                    actions.addFilter('filterNamespace', item.name);
                  }
                }}
                onExclude={() => {
                  if (value.filterNoNamespace?.includes(item.name)) {
                    actions.removeFilter('filterNoNamespace', item.name);
                  } else {
                    actions.addFilter('filterNoNamespace', item.name);
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

export function getNamespacesFiltersLength(value: FiltersInternal) {
  return (
    (value.filterNamespace?.length ?? 0) +
    (value.filterNoNamespace?.length ?? 0)
  );
}
