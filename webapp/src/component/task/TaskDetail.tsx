import { Formik } from 'formik';
import { T, useTranslate } from '@tolgee/react';
import { Box, Button, DialogTitle, styled, Typography } from '@mui/material';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { TextField } from 'tg.component/common/form/fields/TextField';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';

import { TaskDatePicker } from './TaskDatePicker';
import { UserSearchSelect } from './assigneeSelect/UserSearchSelect';
import { TaskLabel } from './TaskLabel';
import { messageService } from 'tg.service/MessageService';
import { TaskInfoItem } from './TaskInfoItem';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { TaskScope } from './TaskScope';

type TaskModel = components['schemas']['TaskModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

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
  width: min(85vw, 800px);
`;

const StyledTopPart = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5, 3)};
  grid-template-columns: 1fr 1fr 1fr;
  align-items: start;
`;

const StyledActions = styled('div')`
  display: flex;
  gap: 8px;
  padding-top: 24px;
  justify-content: end;
`;

type Props = {
  task: TaskModel;
  onClose: () => void;
  project: SimpleProjectModel;
};

export const TaskDetail = ({ task, onClose, project }: Props) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();

  const taskLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/{taskId}',
    method: 'get',
    path: { projectId: project.id, taskId: task.id },
  });

  const updateLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskId}',
    method: 'put',
    invalidatePrefix: ['/v2/projects/{projectId}/tasks', '/v2/user-tasks'],
  });

  const data = taskLoadable.data ?? task;

  return (
    <>
      <StyledMainTitle>
        <T keyName="task_detail_title" />
      </StyledMainTitle>

      <StyledSubtitle>
        <TaskLabel task={data} />
      </StyledSubtitle>
      <Formik
        initialValues={{
          name: data.name,
          description: data.description,
          dueDate: data.dueDate,
          assignees: data.assignees,
        }}
        enableReinitialize
        validationSchema={Validation.UPDATE_TASK_FORM(t)}
        onSubmit={(values, actions) => {
          updateLoadable.mutate(
            {
              path: { projectId: project.id, taskId: task.id },
              content: {
                'application/json': {
                  name: values.name,
                  description: values.description,
                  dueDate: values.dueDate,
                  assignees: values.assignees.map((u) => u.id),
                },
              },
            },
            {
              onSuccess() {
                messageService.success(
                  <T keyName="update_task_success_message" />
                );
                onClose();
              },
              onSettled() {
                actions.setSubmitting(false);
              },
            }
          );
        }}
      >
        {({ values, handleSubmit, setFieldValue, isSubmitting, dirty }) => (
          <StyledForm onSubmit={handleSubmit}>
            <StyledTopPart>
              <TextField
                name="name"
                label={t('task_detail_field_name')}
                fullWidth
              />
              <UserSearchSelect
                label={t('task_detail_field_assignees')}
                value={values.assignees}
                onChange={(value) => setFieldValue('assignees', value)}
                project={project}
              />
              <TaskDatePicker
                value={values.dueDate ?? null}
                onChange={(value) => setFieldValue('dueDate', value)}
                label={t('task_detail_field_due_date')}
              />
            </StyledTopPart>
            <TextField
              label={t('task_detail_field_description')}
              name="description"
              multiline
              minRows={3}
            />

            <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
              {t('task_detail_scope_title')}
            </Typography>

            <TaskScope task={task} />

            <Box display="grid" gridTemplateColumns="repeat(4, 1fr)" pt="20px">
              <TaskInfoItem
                label={t('task_detail_author_label')}
                value={task.author?.name ?? task.author?.username}
              />
              <TaskInfoItem
                label={t('task_detail_created_at_label')}
                value={formatDate(task.createdAt)}
              />
              <TaskInfoItem
                label={t('task_detail_closed_at_label')}
                value={task.closedAt ? formatDate(task.closedAt) : null}
              />
              <TaskInfoItem
                label={t('task_detail_project_label')}
                value={project.name}
              />
            </Box>

            <StyledActions>
              <Button onClick={onClose}>{t('global_cancel_button')}</Button>
              <LoadingButton
                color="primary"
                variant="contained"
                type="submit"
                loading={isSubmitting}
                disabled={!dirty}
              >
                {t('task_detail_submit_button')}
              </LoadingButton>
            </StyledActions>
          </StyledForm>
        )}
      </Formik>
    </>
  );
};
