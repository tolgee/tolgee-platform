import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Menu } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { FilterItem } from './FilterItem';
import { FiltersInternal, FilterActions } from './tools';

type PluralFilter = 'filterIsPlural' | 'filterIsNotPlural';

type Props = {
  value: FiltersInternal;
  actions: FilterActions;
};

export const SubfilterPlurals = ({ value, actions }: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  const toggle = (filter: PluralFilter) =>
    value[filter] ? actions.removeFilter(filter) : actions.addFilter(filter);

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_plurals', 'Plurals')}
        onClick={() => setOpen(true)}
        selected={Boolean(getPluralFiltersLength(value))}
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
          <Box display="grid">
            <FilterItem
              label={t('translations_filter_plural_keys', 'Plural keys')}
              selected={Boolean(value.filterIsPlural)}
              onClick={() => toggle('filterIsPlural')}
            />
            <FilterItem
              label={t(
                'translations_filter_non_plural_keys',
                'Non-plural keys'
              )}
              selected={Boolean(value.filterIsNotPlural)}
              onClick={() => toggle('filterIsNotPlural')}
            />
          </Box>
        </Menu>
      )}
    </>
  );
};

export function getPluralFiltersLength(value: FiltersInternal) {
  return (
    Number(Boolean(value.filterIsPlural)) +
    Number(Boolean(value.filterIsNotPlural))
  );
}

export function getPluralFiltersName(value: FiltersInternal) {
  if (value.filterIsPlural) {
    return (
      <T keyName="translations_filter_plural_keys" defaultValue="Plural keys" />
    );
  }

  if (value.filterIsNotPlural) {
    return (
      <T
        keyName="translations_filter_non_plural_keys"
        defaultValue="Non-plural keys"
      />
    );
  }
}
