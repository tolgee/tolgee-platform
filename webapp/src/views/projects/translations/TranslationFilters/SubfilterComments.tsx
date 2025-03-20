import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Menu } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { FiltersInternal, FilterActions } from './tools';
import { FilterItem } from './FilterItem';

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
};

export const SubfilterComments = ({ value, actions }: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  function handleUnresolvedSetValue(newValue: boolean) {
    if (newValue) {
      actions.addFilter('filterHasUnresolvedComments');
    } else {
      actions.removeFilter('filterHasUnresolvedComments');
    }
  }

  function handleSetValue(newValue: boolean) {
    if (newValue) {
      actions.addFilter('filterHasComments');
    } else {
      actions.removeFilter('filterHasComments');
    }
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_comments')}
        onClick={() => setOpen(true)}
        selected={Boolean(getCommentsFiltersLength(value))}
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
          }}
          slotProps={{ paper: { style: { minWidth: 250 } } }}
        >
          <FilterItem
            label={t('translation_filters_comments_unresolved')}
            selected={Boolean(value.filterHasUnresolvedComments)}
            onClick={() =>
              handleUnresolvedSetValue(!value.filterHasUnresolvedComments)
            }
          />
          <FilterItem
            label={t('translation_filters_comments_any')}
            selected={Boolean(value.filterHasComments)}
            onClick={() => handleSetValue(!value.filterHasComments)}
          />
        </Menu>
      )}
    </>
  );
};

export function getCommentsFiltersLength(value: FiltersInternal) {
  return (
    Number(value.filterHasUnresolvedComments !== undefined) +
    Number(value.filterHasComments !== undefined)
  );
}

export function getCommentsFiltersName(value: FiltersInternal) {
  if (value.filterHasUnresolvedComments) {
    return <T keyName="translation_filters_comments_unresolved" />;
  }

  if (value.filterHasComments) {
    return <T keyName="translation_filters_comments_any" />;
  }
}
