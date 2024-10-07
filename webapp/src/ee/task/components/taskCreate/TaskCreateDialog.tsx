import {
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogTitle,
  ListItemText,
  MenuItem,
  styled,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';
import { useState } from 'react';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { useTaskTypeTranslation } from 'tg.translationTools/useTaskTranslation';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { Select as FormSelect } from 'tg.component/common/form/fields/Select';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { FiltersType } from 'tg.component/translation/translationFilters/tools';
import { TranslationFilters } from 'tg.component/translation/translationFilters/TranslationFilters';
import { Select } from 'tg.component/common/Select';
import { User } from 'tg.component/UserAccount';

import { TaskDatePicker } from '../TaskDatePicker';
import { TaskPreview } from './TaskPreview';
import {
  TranslationStateFilter,
  TranslationStateType,
} from './TranslationStateFilter';

type TaskType = components['schemas']['TaskModel']['type'];
type LanguageModel = components['schemas']['LanguageModel'];

const TASK_TYPES: TaskType[] = ['TRANSLATE', 'REVIEW'];

const StyledMainTitle = styled(DialogTitle)`
  padding-bottom: 0px;
`;

const StyledSubtitle = styled('div')`
  padding: ${({ theme }) => theme.spacing(0, 3, 2, 3)};
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledContainer = styled('div')`
  display: grid;
  padding: ${({ theme }) => theme.spacing(3)};
  gap: ${({ theme }) => theme.spacing(0.5, 3)};
  padding-top: ${({ theme }) => theme.spacing(1)};
  width: min(calc(100vw - 64px), 800px);
`;

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

const StyledActions = styled('div')`
  display: flex;
  gap: 8px;
  padding-top: 24px;
  justify-content: end;
`;

export type InitialValues = {
  type: TaskType;
  name: string;
  description: string;
  languages: number[];
  dueDate: number;
  languageAssignees: Record<number, User[]>;
  selection: number[];
};

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  projectId: number;
  allLanguages: LanguageModel[];
  initialValues?: Partial<InitialValues>;
};

export const TaskCreateDialog = ({
  open,
  onClose,
  onFinished,
  projectId,
  allLanguages,
  initialValues,
}: Props) => {
  const { t } = useTranslate();

  const translateTaskType = useTaskTypeTranslation();

  const createTasksLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/create-multiple-tasks',
    method: 'post',
    invalidatePrefix: ['/v2/projects/{projectId}/tasks', '/v2/user-tasks'],
  });

  const [filters, setFilters] = useState<FiltersType>({});
  const [stateFilters, setStateFilters] = useState<TranslationStateType[]>([]);
  const [languages, setLanguages] = useState(initialValues?.languages ?? []);

  const selectedLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/translations/select-all',
    method: 'get',
    path: { projectId },
    query: {
      ...filters,
      languages: allLanguages.map((l) => l.tag),
    },
    options: {
      enabled: !initialValues?.selection,
    },
  });

  const selectedKeys =
    initialValues?.selection ?? selectedLoadable.data?.ids ?? [];

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg">
      <StyledMainTitle>
        <T keyName="batch_operation_create_task_title" />
      </StyledMainTitle>
      <StyledSubtitle>
        <T
          keyName="batch_operation_create_task_keys_subtitle"
          params={{ value: selectedKeys.length }}
        />
      </StyledSubtitle>

      <Formik
        initialValues={{
          type: initialValues?.type ?? 'TRANSLATE',
          name: initialValues?.name ?? '',
          description: initialValues?.description ?? '',
          dueDate: initialValues?.dueDate ?? undefined,
          assignees: initialValues?.languageAssignees ?? {},
        }}
        validationSchema={Validation.CREATE_TASK_FORM(t)}
        onSubmit={async (values) => {
          const data = languages.map((languageId) => ({
            type: values.type,
            name: values.name,
            description: values.description,
            languageId: languageId,
            dueDate: values.dueDate,
            assignees: values.assignees[languageId]?.map((u) => u.id) ?? [],
            keys: selectedKeys,
          }));
          createTasksLoadable.mutate(
            {
              path: { projectId },
              query: {
                filterState: stateFilters.filter((i) => i !== 'OUTDATED'),
                filterOutdated: stateFilters.includes('OUTDATED'),
              },
              content: {
                'application/json': { tasks: data },
              },
            },
            {
              onSuccess() {
                messageService.success(
                  <T
                    keyName="create_task_success_message"
                    params={{ count: languages.length }}
                  />
                );
                onFinished();
              },
            }
          );
        }}
      >
        {({ values, setFieldValue, submitForm }) => {
          return (
            <StyledContainer>
              <StyledTopPart>
                <FormSelect
                  label={t('create_task_field_type')}
                  name="type"
                  size="small"
                  renderValue={(v) => translateTaskType(v)}
                  fullWidth
                  data-cy="create-task-field-type"
                >
                  {TASK_TYPES.map((v) => (
                    <MenuItem
                      key={v}
                      value={v}
                      data-cy="create-task-field-type-item"
                    >
                      {translateTaskType(v)}
                    </MenuItem>
                  ))}
                </FormSelect>
                <TextField
                  name="name"
                  label={t('create_task_field_name')}
                  data-cy="create-task-field-name"
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
                  style={{ display: 'grid' }}
                  renderValue={
                    ((langIds: number[]) =>
                      langIds
                        .map(
                          (id) => allLanguages?.find((l) => l.id === id)?.name
                        )
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
                <TaskDatePicker
                  label={t('create_task_field_due_date')}
                  value={values.dueDate ?? null}
                  onChange={(value) => setFieldValue('dueDate', value)}
                />
              </StyledTopPart>
              <TextField
                label={t('create_task_field_description')}
                data-cy="create-task-field-description"
                name="description"
                multiline
                minRows={3}
              />

              <Typography variant="subtitle2" mt={2}>
                {t('create_task_tasks_and_assignees_title')}
              </Typography>
              <StyledFilters my={1}>
                {!initialValues?.selection && (
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
                    />
                  ))}
                </Box>
              )}
              <StyledActions>
                <Button onClick={onClose}>{t('global_cancel_button')}</Button>
                <LoadingButton
                  disabled={!languages.length}
                  onClick={submitForm}
                  color="primary"
                  variant="contained"
                  loading={createTasksLoadable.isLoading}
                  data-cy="create-task-submit"
                >
                  {t('create_task_submit_button')}
                </LoadingButton>
              </StyledActions>
            </StyledContainer>
          );
        }}
      </Formik>
    </Dialog>
  );
};
