import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { CompactListSubheader } from 'tg.component/ListComponents';

import { FilterItem } from 'tg.views/projects/translations/TranslationFilters/FilterItem';
import { SubfilterTasksProps } from '../../../eeSetup/EeModuleType';
import {
  FiltersInternal,
  TASK_STATUSES,
  TaskStatusFilter,
} from 'tg.views/projects/translations/TranslationFilters/tools';
import { TASK_TYPES, TaskType } from 'tg.service/apiSchemaTypes';
import {
  DEFAULT_PINNED_STATUS,
  PINNED_TYPE_STATUSES,
  pinnedTaskStatus,
} from '../hooks/useTaskCreationFilters';
import { useTaskTypeTranslation } from 'tg.translationTools/useTaskTranslation';
import { TaskStatusFilterName } from './TaskStatusFilterName';

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
  const [openType, setOpenType] = useState<TaskType>();
  const [typeAnchorEl, setTypeAnchorEl] = useState<HTMLElement | null>(null);
  const anchorEl = useRef<HTMLElement>(null);
  const [expanded, setExpanded] = useState(
    value.filterTaskLanguage !== undefined
  );
  const disabled = taskCreation && selectedLanguages.length === 0;
  const byType = value.filterTaskStatus ?? {};

  function statusOfPinnedDefault(type: TaskType) {
    return type === pinnedTaskType ? DEFAULT_PINNED_STATUS : undefined;
  }

  /** The type being created is always constrained, whether or not the user picked a condition. */
  function statusOf(type: TaskType) {
    if (type === pinnedTaskType) {
      return pinnedTaskStatus(byType[type]);
    }
    return byType[type];
  }

  function statusesFor(type: TaskType) {
    if (type === pinnedTaskType) {
      return PINNED_TYPE_STATUSES;
    }
    return TASK_STATUSES;
  }

  function allowsAny(type: TaskType) {
    return type !== pinnedTaskType;
  }

  function selectStatus(type: TaskType, status: TaskStatusFilter | undefined) {
    const next = { ...byType };
    // storing the pinned type's derived default would outlive the form type it came from
    if (status === undefined || status === statusOfPinnedDefault(type)) {
      delete next[type];
    } else {
      next[type] = status;
    }
    actions.setFilters({
      ...value,
      filterTaskStatus: Object.keys(next).length ? next : undefined,
    });
  }

  function toggleFilterLanguage(
    newValue: FiltersInternal['filterTaskLanguage']
  ) {
    actions.setFilters({
      ...value,
      filterTaskLanguage:
        newValue === value.filterTaskLanguage ? undefined : newValue,
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
          anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
          transformOrigin={{ vertical: 'top', horizontal: 'left' }}
          onClose={() => {
            setOpenType(undefined);
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
            <CompactListSubheader>
              {t('translation_filters_task_type_title')}
            </CompactListSubheader>
            {TASK_TYPES.map((type) => (
              <SubmenuItem
                key={type}
                data-cy="translations-filter-task-type"
                data-cy-type={type}
                label={translateTaskType(type)}
                selected={statusOf(type) !== undefined}
                disabled={disabled}
                onClick={(e) => {
                  setTypeAnchorEl(e.currentTarget);
                  setOpenType(type);
                }}
                open={openType === type}
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
                    {selectedLanguages?.map((lang) => (
                      <FilterItem
                        data-cy="translations-filter-apply-for-language"
                        key={lang.id}
                        label={lang.name}
                        selected={value.filterTaskLanguage === lang.tag}
                        onClick={() => toggleFilterLanguage(lang.tag)}
                        exclusive
                      />
                    ))}
                  </>
                )}
                <MenuItem
                  data-cy="translations-filter-apply-for-expand"
                  role="button"
                  onClick={() => setExpanded((value) => !value)}
                  sx={{ display: 'flex', justifyContent: 'center' }}
                >
                  {expanded ? <ChevronUp /> : <ChevronDown />}
                </MenuItem>
              </>
            )}
          </Box>
        </Menu>
      )}
      {openType && typeAnchorEl && (
        <Menu
          open={true}
          anchorEl={typeAnchorEl}
          anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
          transformOrigin={{ vertical: 'top', horizontal: 'left' }}
          onClose={() => setOpenType(undefined)}
          // the condition list stays open while the other task type is set, so its modal layer
          // must let clicks through to the type rows behind it
          hideBackdrop
          disableAutoFocus
          disableEnforceFocus
          slotProps={{
            root: { sx: { pointerEvents: 'none' } },
            paper: { style: { minWidth: 220 }, sx: { pointerEvents: 'auto' } },
          }}
        >
          <Box display="grid">
            {allowsAny(openType) && (
              <FilterItem
                data-cy="translations-filter-task-status"
                data-cy-status="ANY"
                label={t('translation_filters_task_status_any')}
                selected={statusOf(openType) === undefined}
                onClick={() => selectStatus(openType, undefined)}
                exclusive
              />
            )}
            {statusesFor(openType).map((status) => (
              <FilterItem
                key={status}
                data-cy="translations-filter-task-status"
                data-cy-status={status}
                label={<TaskStatusFilterName status={status} />}
                selected={statusOf(openType) === status}
                onClick={() => selectStatus(openType, status)}
                exclusive
              />
            ))}
          </Box>
        </Menu>
      )}
    </>
  );
};

export function getTaskFiltersLength(value: FiltersInternal) {
  return Object.keys(value.filterTaskStatus ?? {}).length;
}

export function getTaskFiltersName(value: FiltersInternal) {
  const entries = Object.entries(value.filterTaskStatus ?? {});
  if (entries.length !== 1) {
    return undefined;
  }
  const [type, status] = entries[0] as [TaskType, TaskStatusFilter];
  return <TaskStatusFilterName status={status} taskType={type} />;
}
