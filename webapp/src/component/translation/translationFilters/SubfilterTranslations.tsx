import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Divider, Menu } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { FilterItem } from './FilterItem';
import {
  type FiltersInternal,
  type TranslationStateType,
  type FilterActions,
} from 'tg.views/projects/translations/context/services/useTranslationFilterService';
import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { useStateTranslation } from 'tg.translationTools/useStateTranslation';

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
};

const states: TranslationStateType[] = [
  'OUTDATED',
  ...(Object.keys(TRANSLATION_STATES) as TranslationStateType[]),
];

export const SubfilterTranslations = ({ value, actions, projectId }: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);
  const translateState = useStateTranslation();

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
            {states.map((state) => {
              return (
                <FilterItem
                  key={state}
                  label={translateState(state)}
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
