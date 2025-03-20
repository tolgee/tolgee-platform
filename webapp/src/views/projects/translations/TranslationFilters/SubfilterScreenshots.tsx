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

export const SubfilterScreenshots = ({ value, actions, projectId }: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_screenshots')}
        onClick={() => setOpen(true)}
        selected={Boolean(getScreenshotFiltersLength(value))}
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
              label={t('translations_filter_no_screenshots')}
              selected={Boolean(value.filterHasNoScreenshot)}
              onClick={() => {
                if (value.filterHasNoScreenshot) {
                  actions.removeFilter('filterHasNoScreenshot');
                } else {
                  actions.addFilter('filterHasNoScreenshot');
                }
              }}
            />
            <FilterItem
              label={t('translations_filter_with_screenshots')}
              selected={Boolean(value.filterHasScreenshot)}
              onClick={() => {
                if (value.filterHasScreenshot) {
                  actions.removeFilter('filterHasScreenshot');
                } else {
                  actions.addFilter('filterHasScreenshot');
                }
              }}
            />
          </Box>
        </Menu>
      )}
    </>
  );
};

export function getScreenshotFiltersLength(value: FiltersInternal) {
  return (
    Number(Boolean(value.filterHasScreenshot)) +
    Number(Boolean(value.filterHasNoScreenshot))
  );
}

export function getScreenshotFiltersName(value: FiltersInternal) {
  if (value.filterHasScreenshot) {
    return <T keyName="translations_filter_with_screenshots" />;
  }

  if (value.filterHasNoScreenshot) {
    return <T keyName="translations_filter_no_screenshots" />;
  }
}
