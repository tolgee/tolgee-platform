import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { useStateTranslation } from 'tg.translationTools/useStateTranslation';
import { CompactListSubheader } from 'tg.component/ListComponents';

import { FiltersInternal, FilterActions, TranslationStateType } from './tools';
import { FilterItem } from './FilterItem';
import { LanguageModel } from './tools';
import { StateIndicator } from './StateIndicator';

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
  selectedLanguages: LanguageModel[];
};

const states: TranslationStateType[] = [
  'UNTRANSLATED',
  'TRANSLATED',
  'REVIEWED',
  'AUTO_TRANSLATED',
  'OUTDATED',
  'DISABLED',
];

export const SubfilterTranslations = ({
  value,
  actions,
  selectedLanguages,
}: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);
  const translateState = useStateTranslation();
  const [expanded, setExpanded] = useState(
    value.filterTranslationLanguage !== undefined
  );

  function toggleFilterLanguage(
    newValue: FiltersInternal['filterTranslationLanguage']
  ) {
    actions.setFilters({
      ...value,
      filterTranslationLanguage:
        newValue === value.filterTranslationLanguage ? undefined : newValue,
    });
  }

  function toggleFilterState(newValue: TranslationStateType) {
    if (value.filterTranslationState?.includes(newValue)) {
      actions.removeFilter('filterTranslationState', newValue);
    } else {
      actions.addFilter('filterTranslationState', newValue);
    }
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_translations')}
        onClick={() => setOpen(true)}
        selected={Boolean(getTranslationFiltersLength(value))}
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
            {states.map((state) => {
              return (
                <FilterItem
                  key={state}
                  label={translateState(state)}
                  selected={Boolean(
                    value.filterTranslationState?.includes(state)
                  )}
                  onClick={() => toggleFilterState(state)}
                  indicator={<StateIndicator state={state} />}
                />
              );
            })}
            <CompactListSubheader>
              <Box display="flex" justifyContent="space-between">
                <Box>{t('translations_filter_languages_select_title')}</Box>
              </Box>
            </CompactListSubheader>
            <FilterItem
              data-cy="translations-filter-apply-no-base"
              label={t('translations_filter_languages_no_base')}
              selected={value.filterTranslationLanguage === undefined}
              onClick={() => toggleFilterLanguage(undefined)}
              exclusive
            />
            {expanded && (
              <>
                <FilterItem
                  data-cy="translations-filter-apply-for-all"
                  label={t('translations_filter_languages_all')}
                  selected={value.filterTranslationLanguage === true}
                  onClick={() => toggleFilterLanguage(true)}
                  exclusive
                />
                {selectedLanguages?.map((lang) => {
                  return (
                    <FilterItem
                      data-cy="translations-filter-apply-for-language"
                      key={lang.id}
                      label={lang.name}
                      selected={value.filterTranslationLanguage === lang.tag}
                      onClick={() => toggleFilterLanguage(lang.tag)}
                      exclusive
                    />
                  );
                })}
              </>
            )}
            <MenuItem
              data-cy="translations-filter-apply-for-expand"
              role="button"
              onClick={() => setExpanded((value) => !value)}
              sx={{
                display: 'flex',
                justifyContent: 'center',
              }}
            >
              {expanded ? <ChevronUp /> : <ChevronDown />}
            </MenuItem>
          </Box>
        </Menu>
      )}
    </>
  );
};

export function getTranslationFiltersLength(value: FiltersInternal) {
  return value.filterTranslationState?.length ?? 0;
}

const TranslatedState = (props: { state: TranslationStateType }) => {
  const translateState = useStateTranslation();
  return <>{translateState(props.state)}</>;
};

export function getTranslationFiltersName(value: FiltersInternal) {
  if (value.filterTranslationState?.length) {
    return <TranslatedState state={value.filterTranslationState[0]} />;
  }
}
