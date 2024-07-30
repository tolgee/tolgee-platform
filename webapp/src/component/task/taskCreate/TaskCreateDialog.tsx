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

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { useTaskTranslation } from 'tg.translationTools/useTaskTranslation';
import { TaskPreview } from 'tg.views/projects/translations/BatchOperations/components/TaskPreview';
import LoadingButton from 'tg.component/common/form/LoadingButton';

import { User } from '../assigneeSelect/types';
import { TaskDatePicker } from '../TaskDatePicker';
import { Select } from 'tg.component/common/form/fields/Select';
import { TextField } from 'tg.component/common/form/fields/TextField';

type TaskType = components['schemas']['TaskModel']['type'];
type ProjectModel = components['schemas']['ProjectModel'];
type LanguageModel = components['schemas']['LanguageModel'];

const TASK_TYPES: TaskType[] = ['TRANSLATE', 'REVIEW'];

const StyledMainTitle = styled(DialogTitle)`
  padding-bottom: 0px;
`;

const StyledSubtitle = styled('div')`
  padding: ${({ theme }) => theme.spacing(0, 3, 2, 3)};
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledForm = styled('form')`
  display: grid;
  padding: ${({ theme }) => theme.spacing(3)};
  gap: ${({ theme }) => theme.spacing(0.5, 3)};
  padding-top: ${({ theme }) => theme.spacing(1)};
  width: min(90vw, 800px);
`;

const StyledTopPart = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5, 3)};
  grid-template-columns: 3fr 5fr;
  align-items: start;
`;

const StyledActions = styled('div')`
  display: flex;
  gap: 8px;
  padding-top: 24px;
  justify-content: end;
`;

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  selection: number[];
  initialLanguages: number[];
  project: ProjectModel;
  allLanguages: LanguageModel[];
};

export const TaskCreateDialog = ({
  open,
  onClose,
  onFinished,
  selection,
  initialLanguages,
  project,
  allLanguages,
}: Props) => {
  const { t } = useTranslate();

  const translateTaskType = useTaskTranslation();

  const createTasksLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/create-multiple',
    method: 'post',
  });

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg">
      <StyledMainTitle>
        <T keyName="batch_operation_create_task_title" />
      </StyledMainTitle>
      <StyledSubtitle>
        <T
          keyName="batch_operation_create_task_keys_subtitle"
          params={{ value: selection.length }}
        />
      </StyledSubtitle>

      <Formik
        initialValues={{
          type: 'TRANSLATE' as TaskType,
          name: '',
          description: '',
          languages: initialLanguages,
          dueDate: undefined as number | undefined,
          assignees: {} as Record<string, User[]>,
        }}
        validationSchema={Validation.CREATE_TASK_FORM(t)}
        onSubmit={async (values, actions) => {
          const data = values.languages.map((languageId) => ({
            type: values.type,
            name: values.name,
            description: values.description,
            languageId: languageId,
            dueDate: values.dueDate,
            assignees: values.assignees[languageId]?.map((u) => u.id) ?? [],
            keys: selection,
          }));
          createTasksLoadable.mutate(
            {
              path: { projectId: project.id },
              content: {
                'application/json': { tasks: data },
              },
            },
            {
              onSuccess() {
                messageService.success(
                  <T
                    keyName="create_task_success_message"
                    params={{ count: values.languages.length }}
                  />
                );
                onFinished();
              },
            }
          );
        }}
      >
        {({ values, handleSubmit, setFieldValue }) => {
          return (
            <StyledForm onSubmit={handleSubmit}>
              <StyledTopPart>
                <Select
                  label={t('create_task_field_type')}
                  name="type"
                  size="small"
                  renderValue={(v) => translateTaskType(v)}
                  fullWidth
                >
                  {TASK_TYPES.map((v) => (
                    <MenuItem key={v} value={v}>
                      {translateTaskType(v)}
                    </MenuItem>
                  ))}
                </Select>
                <TextField
                  name="name"
                  label={t('create_task_field_name')}
                  fullWidth
                />
                <Select
                  label={t('create_task_field_languages')}
                  name="languages"
                  size="small"
                  fullWidth
                  multiple
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
                    <MenuItem key={lang.id} value={lang.id} dense>
                      <Checkbox
                        sx={{ marginLeft: -0.75 }}
                        checked={values.languages.includes(lang.id)}
                        size="small"
                      />
                      <ListItemText primary={lang.name} />
                    </MenuItem>
                  ))}
                </Select>

                <TaskDatePicker
                  value={values.dueDate ?? null}
                  onChange={(value) => setFieldValue('dueDate', value)}
                  label={t('create_task_field_due_date')}
                />
              </StyledTopPart>
              <TextField
                label={t('create_task_field_description')}
                name="description"
                multiline
                minRows={3}
              />
              <Typography variant="subtitle2" sx={{ mt: 2 }}>
                {t('create_task_tasks_and_assignees_title')}
              </Typography>

              {allLanguages && (
                <Box display="grid" gap={2} mt={1}>
                  {values.languages?.map((language) => (
                    <TaskPreview
                      key={language}
                      language={allLanguages.find((l) => l.id === language)!}
                      type={values.type}
                      keys={selection}
                      assigness={values.assignees[language] ?? []}
                      onUpdateAssignees={(users) => {
                        setFieldValue(`assignees[${language}]`, users);
                      }}
                    />
                  ))}
                </Box>
              )}

              <StyledActions>
                <Button onClick={onClose}>{t('global_cancel_button')}</Button>
                <LoadingButton
                  color="primary"
                  variant="contained"
                  type="submit"
                  loading={createTasksLoadable.isLoading}
                >
                  {t('create_task_submit_button')}
                </LoadingButton>
              </StyledActions>
            </StyledForm>
          );
        }}
      </Formik>
    </Dialog>
  );
};
