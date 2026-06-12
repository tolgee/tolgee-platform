import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Menu } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { FilterItem } from './FilterItem';
import { FiltersInternal, FilterActions } from './tools';

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
};

export const SubfilterDescription = ({ value, actions, projectId }: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_description')}
        onClick={() => setOpen(true)}
        selected={Boolean(getDescriptionFiltersLength(value))}
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
              label={t('translations_filter_no_description')}
              selected={Boolean(value.filterHasNoDescription)}
              onClick={() => {
                if (value.filterHasNoDescription) {
                  actions.removeFilter('filterHasNoDescription');
                } else {
                  actions.addFilter('filterHasNoDescription');
                }
              }}
            />
            <FilterItem
              label={t('translations_filter_with_description')}
              selected={Boolean(value.filterHasDescription)}
              onClick={() => {
                if (value.filterHasDescription) {
                  actions.removeFilter('filterHasDescription');
                } else {
                  actions.addFilter('filterHasDescription');
                }
              }}
            />
          </Box>
        </Menu>
      )}
    </>
  );
};

export function getDescriptionFiltersLength(value: FiltersInternal) {
  return (
    Number(Boolean(value.filterHasDescription)) +
    Number(Boolean(value.filterHasNoDescription))
  );
}

export function getDescriptionFiltersName(value: FiltersInternal) {
  if (value.filterHasDescription) {
    return <T keyName="translations_filter_with_description" />;
  }

  if (value.filterHasNoDescription) {
    return <T keyName="translations_filter_no_description" />;
  }
}
