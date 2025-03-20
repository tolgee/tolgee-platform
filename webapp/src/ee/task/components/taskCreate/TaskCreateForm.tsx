import {
  Box,
  Checkbox,
  ListItemText,
  MenuItem,
  styled,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { Select as FormSelect } from 'tg.component/common/form/fields/Select';
import { useTaskTypeTranslation } from 'tg.translationTools/useTaskTranslation';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { TaskDatePicker } from '../TaskDatePicker';
import { TranslationStateFilter } from './TranslationStateFilter';
import { TaskPreview } from './TaskPreview';
import { Field, useFormikContext } from 'formik';
import {
  FilterActions,
  FiltersType,
} from 'tg.views/projects/translations/TranslationFilters/tools';
import { Select } from 'tg.component/common/Select';
import { useEffect } from 'react';
import { TranslationStateType } from 'tg.translationTools/useStateTranslation';
import { useApiQueries } from 'tg.service/http/useQueryApi';
import { stringHash } from 'tg.fixtures/stringHash';
import { StateType } from 'tg.constants/translationStates';
import { TranslationFilters } from 'tg.views/projects/translations/TranslationFilters/TranslationFilters';

type TaskType = components['schemas']['TaskModel']['type'];
type LanguageModel = components['schemas']['LanguageModel'];
type KeysScopeView = components['schemas']['KeysScopeView'];

const TASK_TYPES: TaskType[] = ['TRANSLATE', 'REVIEW'];

export const DEFAULT_STATE_FILTERS_TRANSLATE: StateType[] = ['UNTRANSLATED'];
export const DEFAULT_STATE_FILTERS_REVIEW: StateType[] = ['TRANSLATED'];

const StyledTopPart = styled(Box)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5, 2)};
  grid-template-columns: 3fr 5fr;
  align-items: start;
  ${({ theme }) => theme.breakpoints.down('sm')} {
    grid-template-columns: 1fr;
  }
`;

const StyledFilters = styled(Box)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5, 2)};
  grid-template-columns: 250px 250px 2fr;
  ${({ theme }) => theme.breakpoints.down('sm')} {
    grid-template-columns: 1fr;
    gap: ${({ theme }) => theme.spacing(2)};
  }
`;

type Props = {
  selectedKeys: number[];
  disabled?: boolean;
  languages: number[];
  setLanguages: (languages: number[]) => void;
  allLanguages: LanguageModel[];
  filters: FiltersType;
  filterActions?: FilterActions;
  stateFilters: TranslationStateType[];
  setStateFilters: (filters: TranslationStateType[]) => void;
  projectId: number;
  hideDueDate?: boolean;
  hideAssignees?: boolean;
  onScopeChange?: (data: (KeysScopeView | undefined)[]) => void;
};

export const TaskCreateForm = ({
  selectedKeys,
  disabled,
  languages,
  setLanguages,
  allLanguages,
  filters,
  filterActions,
  stateFilters,
  setStateFilters,
  projectId,
  hideDueDate,
  hideAssignees,
  onScopeChange,
}: Props) => {
  const { t } = useTranslate();
  const translateTaskType = useTaskTypeTranslation();

  const { values, setFieldValue } = useFormikContext<any>();

  const taskScopes = useApiQueries(
    languages.map((languageId) => {
      const content = {
        keys: selectedKeys,
        type: values.type,
        languageId,
      };
      return {
        url: '/v2/projects/{projectId}/tasks/calculate-scope',
        method: 'post',
        path: { projectId },
        content: { 'application/json': content },
        query: {
          // @ts-ignore
          hash: stringHash(JSON.stringify(content)),
          filterState: stateFilters.filter(
            (i) => i !== 'OUTDATED' && i !== 'AUTO_TRANSLATED'
          ),
          filterOutdated: stateFilters.includes('OUTDATED'),
        },
      };
    })
  );

  useEffect(() => {
    onScopeChange?.(taskScopes.map((i) => i.data));
  }, [taskScopes.map((i) => String(i.dataUpdatedAt)).join(',')]);

  useEffect(() => {
    // make sure base language is not selected
    const baseLang = allLanguages.find((l) => l.base);
    if (
      values.type === 'TRANSLATE' &&
      baseLang &&
      languages.includes(baseLang.id)
    ) {
      setLanguages(languages.filter((l) => l !== baseLang.id));
    }
  }, [values.type, languages, allLanguages]);

  return (
    <>
      <StyledTopPart>
        <FormSelect
          label={t('create_task_field_type')}
          name="type"
          size="small"
          renderValue={(v) => translateTaskType(v)}
          fullWidth
          data-cy="create-task-field-type"
          disabled={disabled}
        >
          {TASK_TYPES.map((v) => (
            <MenuItem key={v} value={v} data-cy="create-task-field-type-item">
              {translateTaskType(v)}
            </MenuItem>
          ))}
        </FormSelect>
        <TextField
          name="name"
          label={t('form_field_optional', {
            label: t('create_task_field_name'),
          })}
          placeholder={t('task_default_name')}
          data-cy="create-task-field-name"
          disabled={disabled}
          fullWidth
        />
        <Select
          label={t('create_task_field_languages')}
          data-cy="create-task-field-languages"
          value={languages}
          onChange={(e) => setLanguages(e.target.value as number[])}
          size="small"
          fullWidth
          multiple
          disabled={disabled}
          style={{ display: 'grid' }}
          renderValue={
            ((langIds: number[]) =>
              langIds
                .map((id) => allLanguages?.find((l) => l.id === id)?.name)
                .join(', ') ?? '') as any
          }
        >
          {allLanguages?.map((lang) => {
            const isBase = lang.base;
            const isDisabled = isBase && values.type === 'TRANSLATE';
            return (
              <MenuItem
                key={lang.id}
                value={lang.id}
                dense
                data-cy="create-task-field-languages-item"
                disabled={isDisabled}
              >
                <Checkbox
                  sx={{ marginLeft: -0.75 }}
                  checked={languages.includes(lang.id)}
                  size="small"
                />
                <ListItemText
                  primary={
                    lang.name +
                    (isBase ? ` (${t('task_create_base_language_label')})` : '')
                  }
                />
              </MenuItem>
            );
          })}
        </Select>
        {!hideDueDate && (
          <Field name="dueDate">
            {(field, form) => (
              <TaskDatePicker
                disabled={disabled}
                label={t('create_task_field_due_date')}
                value={field.value ?? null}
                onChange={(value) => form.setFieldValue(field.name, value)}
              />
            )}
          </Field>
        )}
      </StyledTopPart>
      <TextField
        disabled={disabled}
        label={t('create_task_field_description')}
        data-cy="create-task-field-description"
        name="description"
        multiline
        minRows={3}
      />

      {!disabled && (
        <>
          <Typography variant="subtitle2" mt={2}>
            {hideAssignees
              ? t('create_task_tasks_summary')
              : t('create_task_tasks_and_assignees_title')}
          </Typography>
          <StyledFilters my={1}>
            {filterActions && (
              <TranslationFilters
                value={filters}
                actions={filterActions}
                selectedLanguages={[]}
                projectId={projectId}
                placeholder={t('create_task_filter_keys_placeholder')}
                filterOptions={{ keyRelatedOnly: true }}
                sx={{ width: '100%', maxWidth: '270px' }}
              />
            )}
            <TranslationStateFilter
              value={stateFilters}
              placeholder={t(
                'create_task_filter_translation_states_placeholder'
              )}
              onChange={setStateFilters}
              sx={{ maxWidth: '270px' }}
            />
          </StyledFilters>

          {allLanguages && (
            <Box display="grid" gap={2} mt={1}>
              {languages?.map((language, i) => (
                <TaskPreview
                  key={language}
                  language={allLanguages.find((l) => l.id === language)!}
                  type={values.type}
                  projectId={projectId}
                  assignees={values.assignees[language] ?? []}
                  onUpdateAssignees={(users) => {
                    setFieldValue(`assignees[${language}]`, users);
                  }}
                  hideAssignees={hideAssignees}
                  scope={taskScopes[i]?.data}
                />
              ))}
            </Box>
          )}
        </>
      )}
    </>
  );
};
