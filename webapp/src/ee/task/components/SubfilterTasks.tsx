import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { CompactListSubheader } from 'tg.component/ListComponents';

import { FilterItem } from 'tg.views/projects/translations/TranslationFilters/FilterItem';
import { SubfilterTasksProps } from 'eeSetup/EeModuleType';
import { FiltersInternal } from 'tg.views/projects/translations/TranslationFilters/tools';

export const SubfilterTasks = ({
  value,
  actions,
  selectedLanguages,
  hideLanguageScope,
}: SubfilterTasksProps) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);
  const [expanded, setExpanded] = useState(
    value.filterTaskLanguage !== undefined
  );
  const disabled = hideLanguageScope && selectedLanguages.length === 0;

  function toggleFilterLanguage(
    newValue: FiltersInternal['filterTaskLanguage']
  ) {
    actions.setFilters({
      ...value,
      filterTaskLanguage:
        newValue === value.filterTaskLanguage ? undefined : newValue,
    });
  }

  function handleToggleHasTask(newValue: boolean) {
    if (newValue) {
      actions.addFilter('filterHasTask');
    } else {
      actions.removeFilter('filterHasTask');
    }
  }

  function handleToggleHasNoTask(newValue: boolean) {
    if (newValue) {
      actions.addFilter('filterHasNoTask');
    } else {
      actions.removeFilter('filterHasNoTask');
    }
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_tasks', 'Tasks')}
        onClick={() => setOpen(true)}
        selected={Boolean(getTaskFiltersLength(value))}
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
            {disabled && (
              <CompactListSubheader>
                {t(
                  'translation_filters_task_select_language_first',
                  'Select target languages first'
                )}
              </CompactListSubheader>
            )}
            <FilterItem
              data-cy="translations-filter-has-been-in-task"
              label={t('translation_filters_has_task', 'Has been in a task')}
              selected={Boolean(value.filterHasTask)}
              disabled={disabled}
              onClick={() => handleToggleHasTask(!value.filterHasTask)}
            />
            <FilterItem
              data-cy="translations-filter-never-in-task"
              label={t('translation_filters_has_no_task', 'Never in a task')}
              selected={Boolean(value.filterHasNoTask)}
              disabled={disabled}
              onClick={() => handleToggleHasNoTask(!value.filterHasNoTask)}
            />
            {!hideLanguageScope && (
              <>
                <CompactListSubheader>
                  <Box display="flex" justifyContent="space-between">
                    <Box>{t('translations_filter_languages_select_title')}</Box>
                  </Box>
                </CompactListSubheader>
                <FilterItem
                  data-cy="translations-filter-apply-no-base"
                  label={t('translations_filter_languages_no_base')}
                  selected={value.filterTaskLanguage === undefined}
                  onClick={() => toggleFilterLanguage(undefined)}
                  exclusive
                />
                {expanded && (
                  <>
                    <FilterItem
                      data-cy="translations-filter-apply-for-all"
                      label={t('translations_filter_languages_all')}
                      selected={value.filterTaskLanguage === true}
                      onClick={() => toggleFilterLanguage(true)}
                      exclusive
                    />
                    {selectedLanguages?.map((lang) => {
                      return (
                        <FilterItem
                          data-cy="translations-filter-apply-for-language"
                          key={lang.id}
                          label={lang.name}
                          selected={value.filterTaskLanguage === lang.tag}
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
              </>
            )}
          </Box>
        </Menu>
      )}
    </>
  );
};

export function getTaskFiltersLength(value: FiltersInternal) {
  return (
    Number(value.filterHasTask !== undefined) +
    Number(value.filterHasNoTask !== undefined)
  );
}

export function getTaskFiltersName(value: FiltersInternal) {
  if (value.filterHasTask) {
    return (
      <T
        keyName="translation_filters_has_task"
        defaultValue="Has been in a task"
      />
    );
  }

  if (value.filterHasNoTask) {
    return (
      <T
        keyName="translation_filters_has_no_task"
        defaultValue="Never in a task"
      />
    );
  }
}
