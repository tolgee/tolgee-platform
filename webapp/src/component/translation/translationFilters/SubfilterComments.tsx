import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Menu } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import {
  type FiltersInternal,
  type FilterActions,
} from 'tg.views/projects/translations/context/services/useTranslationFilterService';
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

  function handleSetValue(newValue: boolean) {
    actions.setFilters({
      ...value,
      filterUnresolvedComments:
        newValue === value.filterUnresolvedComments ? undefined : newValue,
    });
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_comments')}
        onClick={() => setOpen(true)}
        selected={Boolean(getCommentsFiltersLength(value))}
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
            selected={value.filterUnresolvedComments === true}
            onClick={() => handleSetValue(true)}
          />
          <FilterItem
            label={t('translation_filters_comments_resolved')}
            selected={value.filterUnresolvedComments === false}
            onClick={() => handleSetValue(false)}
          />
        </Menu>
      )}
    </>
  );
};

export function getCommentsFiltersLength(value: FiltersInternal) {
  return Number(value.filterUnresolvedComments !== undefined);
}
