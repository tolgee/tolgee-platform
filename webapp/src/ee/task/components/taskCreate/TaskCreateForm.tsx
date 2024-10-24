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
import { TranslationFilters } from 'tg.component/translation/translationFilters/TranslationFilters';
import {
  TranslationStateFilter,
  TranslationStateType,
} from './TranslationStateFilter';
import { TaskPreview } from './TaskPreview';
import { Field, useFormikContext } from 'formik';
import { FiltersType } from 'tg.component/translation/translationFilters/tools';
import { Select } from 'tg.component/common/Select';

type TaskType = components['schemas']['TaskModel']['type'];
type LanguageModel = components['schemas']['LanguageModel'];

const TASK_TYPES: TaskType[] = ['TRANSLATE', 'REVIEW'];

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
  grid-template-columns: 3fr 3fr 2fr;
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
  setFilters?: (filters: FiltersType) => void;
  stateFilters: TranslationStateType[];
  setStateFilters: (filters: TranslationStateType[]) => void;
  projectId: number;
  hideDueDate?: boolean;
  hideAssignees?: boolean;
};

export const TaskCreateForm = ({
  selectedKeys,
  disabled,
  languages,
  setLanguages,
  allLanguages,
  filters,
  setFilters,
  stateFilters,
  setStateFilters,
  projectId,
  hideDueDate,
  hideAssignees,
}: Props) => {
  const { t } = useTranslate();
  const translateTaskType = useTaskTypeTranslation();

  const { values, setFieldValue } = useFormikContext<any>();

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
          label={t('create_task_field_name')}
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
          {allLanguages?.map((lang) => (
            <MenuItem
              key={lang.id}
              value={lang.id}
              dense
              data-cy="create-task-field-languages-item"
            >
              <Checkbox
                sx={{ marginLeft: -0.75 }}
                checked={languages.includes(lang.id)}
                size="small"
              />
              <ListItemText primary={lang.name} />
            </MenuItem>
          ))}
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
            {t('create_task_tasks_and_assignees_title')}
          </Typography>
          <StyledFilters my={1}>
            {setFilters && (
              <TranslationFilters
                value={filters}
                onChange={setFilters}
                selectedLanguages={allLanguages.filter((l) =>
                  languages.includes(l.id)
                )}
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
              {languages?.map((language) => (
                <TaskPreview
                  key={language}
                  language={allLanguages.find((l) => l.id === language)!}
                  type={values.type}
                  keys={selectedKeys}
                  assigness={values.assignees[language] ?? []}
                  onUpdateAssignees={(users) => {
                    setFieldValue(`assignees[${language}]`, users);
                  }}
                  filters={stateFilters}
                  projectId={projectId}
                  hideAssignees={hideAssignees}
                />
              ))}
            </Box>
          )}
        </>
      )}
    </>
  );
};
