import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { CompactListSubheader } from 'tg.component/ListComponents';

import { FiltersInternal, FilterActions } from './tools';
import { FilterItem } from './FilterItem';
import { LanguageModel } from './tools';

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
  selectedLanguages: LanguageModel[];
};

export const SubfilterSuggestions = ({
  value,
  actions,
  selectedLanguages,
}: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);
  const [expanded, setExpanded] = useState(
    value.filterSuggestionLanguage !== undefined
  );

  function toggleFilterLanguage(
    newValue: FiltersInternal['filterSuggestionLanguage']
  ) {
    actions.setFilters({
      ...value,
      filterSuggestionLanguage:
        newValue === value.filterSuggestionLanguage ? undefined : newValue,
    });
  }

  function handleToggleHasSuggestions(newValue: boolean) {
    if (newValue) {
      actions.addFilter('filterHasSuggestions');
    } else {
      actions.removeFilter('filterHasSuggestions');
    }
  }

  function handleToggleHasNoSuggestions(newValue: boolean) {
    if (newValue) {
      actions.addFilter('filterHasNoSuggestions');
    } else {
      actions.removeFilter('filterHasNoSuggestions');
    }
  }
  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_suggestions')}
        onClick={() => setOpen(true)}
        selected={Boolean(getSuggestionsFiltersLength(value))}
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
              label={t('translation_filters_has_suggestions')}
              selected={Boolean(value.filterHasSuggestions)}
              onClick={() =>
                handleToggleHasSuggestions(!value.filterHasSuggestions)
              }
            />
            <FilterItem
              label={t('translation_filters_has_no_suggestions')}
              selected={Boolean(value.filterHasNoSuggestions)}
              onClick={() =>
                handleToggleHasNoSuggestions(!value.filterHasNoSuggestions)
              }
            />
            <CompactListSubheader>
              <Box display="flex" justifyContent="space-between">
                <Box>{t('translations_filter_languages_select_title')}</Box>
              </Box>
            </CompactListSubheader>
            <FilterItem
              data-cy="translations-filter-apply-no-base"
              label={t('translations_filter_languages_no_base')}
              selected={value.filterSuggestionLanguage === undefined}
              onClick={() => toggleFilterLanguage(undefined)}
              exclusive
            />
            {expanded && (
              <>
                <FilterItem
                  data-cy="translations-filter-apply-for-all"
                  label={t('translations_filter_languages_all')}
                  selected={value.filterSuggestionLanguage === true}
                  onClick={() => toggleFilterLanguage(true)}
                  exclusive
                />
                {selectedLanguages?.map((lang) => {
                  return (
                    <FilterItem
                      data-cy="translations-filter-apply-for-language"
                      key={lang.id}
                      label={lang.name}
                      selected={value.filterSuggestionLanguage === lang.tag}
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

export function getSuggestionsFiltersLength(value: FiltersInternal) {
  return (
    Number(value.filterHasSuggestions !== undefined) +
    Number(value.filterHasNoSuggestions !== undefined)
  );
}

export function getSuggestionsFiltersName(value: FiltersInternal) {
  if (value.filterHasSuggestions) {
    return <T keyName="translation_filters_has_suggestions" />;
  }

  if (value.filterHasNoSuggestions) {
    return <T keyName="translation_filters_has_no_suggestions" />;
  }
}
