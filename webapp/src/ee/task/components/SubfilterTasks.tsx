import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { CompactListSubheader } from 'tg.component/ListComponents';

import { FilterItem } from 'tg.views/projects/translations/TranslationFilters/FilterItem';
import { SubfilterTasksProps } from '../../../eeSetup/EeModuleType';
import {
  FiltersInternal,
  TaskStatusFilter,
} from 'tg.views/projects/translations/TranslationFilters/tools';
import { TASK_TYPES, TaskType } from 'tg.service/apiSchemaTypes';
import { useTaskTypeTranslation } from 'tg.translationTools/useTaskTranslation';
import { TaskTypeFilterName } from './TaskTypeFilterName';

export const SubfilterTasks = ({
  value,
  actions,
  selectedLanguages,
  taskCreation,
  pinnedTaskType,
}: SubfilterTasksProps) => {
  const { t } = useTranslate();
  const translateTaskType = useTaskTypeTranslation();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);
  const [expanded, setExpanded] = useState(
    value.filterTaskLanguage !== undefined
  );
  const disabled = taskCreation && selectedLanguages.length === 0;
  const typesDisabled = disabled || value.filterTaskStatus === undefined;

  function toggleFilterLanguage(
    newValue: FiltersInternal['filterTaskLanguage']
  ) {
    actions.setFilters({
      ...value,
      filterTaskLanguage:
        newValue === value.filterTaskLanguage ? undefined : newValue,
    });
  }

  function selectStatus(newValue: TaskStatusFilter) {
    const clearing = !taskCreation && newValue === value.filterTaskStatus;
    actions.setFilters({
      ...value,
      filterTaskStatus: clearing ? undefined : newValue,
      filterTaskType: clearing ? undefined : value.filterTaskType,
    });
  }

  const selectedTypes = value.filterTaskType ?? [];

  function isTypeSelected(type: TaskType) {
    if (type === pinnedTaskType) {
      return true;
    }
    if (selectedTypes.length) {
      return selectedTypes.includes(type);
    }
    return !taskCreation;
  }

  function toggleType(type: TaskType) {
    if (typesDisabled || type === pinnedTaskType) {
      return;
    }
    const current = TASK_TYPES.filter(isTypeSelected);
    const next = current.includes(type)
      ? current.filter((i) => i !== type)
      : [...current, type];
    actions.setFilters({
      ...value,
      filterTaskType: next.length ? next : undefined,
    });
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl}
        label={t('translations_filters_heading_tasks')}
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
                {t('translation_filters_task_select_language_first')}
              </CompactListSubheader>
            )}
            <FilterItem
              data-cy="translations-filter-not-in-open-task"
              label={t('translation_filters_not_in_open_task')}
              selected={value.filterTaskStatus === 'NOT_IN_OPEN_TASK'}
              disabled={disabled}
              onClick={() => selectStatus('NOT_IN_OPEN_TASK')}
              exclusive
            />
            {!taskCreation && (
              <FilterItem
                data-cy="translations-filter-has-been-in-task"
                label={t('translation_filters_has_task')}
                selected={value.filterTaskStatus === 'HAS_BEEN_IN_TASK'}
                onClick={() => selectStatus('HAS_BEEN_IN_TASK')}
                exclusive
              />
            )}
            <FilterItem
              data-cy="translations-filter-never-in-task"
              label={t('translation_filters_has_no_task')}
              selected={value.filterTaskStatus === 'NEVER_IN_TASK'}
              disabled={disabled}
              onClick={() => selectStatus('NEVER_IN_TASK')}
              exclusive
            />
            <CompactListSubheader>
              {t('translation_filters_task_type_title')}
            </CompactListSubheader>
            {TASK_TYPES.map((type) => (
              <FilterItem
                key={type}
                data-cy="translations-filter-task-type"
                data-cy-type={type}
                label={translateTaskType(type)}
                selected={isTypeSelected(type)}
                disabled={typesDisabled || type === pinnedTaskType}
                onClick={() => toggleType(type)}
              />
            ))}
            {!taskCreation && (
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
    Number(value.filterTaskStatus !== undefined) +
    Number(narrowingTaskTypes(value) !== undefined)
  );
}

export function getTaskFiltersName(value: FiltersInternal) {
  switch (value.filterTaskStatus) {
    case 'NOT_IN_OPEN_TASK':
      return <T keyName="translation_filters_not_in_open_task" />;
    case 'HAS_BEEN_IN_TASK':
      return <T keyName="translation_filters_has_task" />;
    case 'NEVER_IN_TASK':
      return <T keyName="translation_filters_has_no_task" />;
  }
  const narrowed = narrowingTaskTypes(value);
  if (narrowed?.length === 1) {
    return <TaskTypeFilterName taskType={narrowed[0]} />;
  }
}

function narrowingTaskTypes(value: FiltersInternal) {
  const types = value.filterTaskType;
  if (!types?.length || types.length >= TASK_TYPES.length) {
    return undefined;
  }
  return types;
}
