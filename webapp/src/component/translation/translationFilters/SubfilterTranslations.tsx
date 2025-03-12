import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Divider, Menu } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { FilterItem } from './FilterItem';
import {
  FiltersInternal,
  type FilterActions,
} from 'tg.views/projects/translations/context/services/useTranslationFilterService';
import { TRANSLATION_STATES } from 'tg.constants/translationStates';

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
};

export const SubfilterTranslations = ({ value, actions, projectId }: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_translations')}
        onClick={() => setOpen(true)}
        selected={Boolean(getTranslationFiltersLength(value))}
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
            {Object.entries(TRANSLATION_STATES).map(([state, params]) => {
              return (
                <FilterItem
                  key={state}
                  label={params.translation}
                  selected={Boolean(
                    value.filterTranslationState?.includes(state)
                  )}
                  onClick={() => {
                    if (value.filterTranslationState?.includes(state)) {
                      actions.removeFilter('filterTranslationState', state);
                    } else if (
                      value.filterNoTranslationState?.includes(state)
                    ) {
                      actions.removeFilter('filterNoTranslationState', state);
                    } else {
                      actions.addFilter('filterTranslationState', state);
                    }
                  }}
                />
              );
            })}

            <Divider />

            <FilterItem
              label={t('translations_filter_ignore_base_language')}
              selected={!value.filterTranslationStateApplyBaseLang}
              onClick={() => {
                actions.setFilters({
                  ...value,
                  filterTranslationStateApplyBaseLang:
                    !value.filterTranslationStateApplyBaseLang || undefined,
                });
              }}
            />
          </Box>
        </Menu>
      )}
    </>
  );
};

export function getTranslationFiltersLength(value: FiltersInternal) {
  return (
    (value.filterTranslationState?.length ?? 0) +
    (value.filterNoTranslationState?.length ?? 0)
  );
}
