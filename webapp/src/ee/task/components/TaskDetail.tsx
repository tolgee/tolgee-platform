import { Formik } from 'formik';
import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  Button,
  DialogTitle,
  IconButton,
  styled,
  Tooltip,
  Typography,
} from '@mui/material';
import { Link } from 'react-router-dom';
import { File06, Translate01 } from '@untitled-ui/icons-react';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { TextField } from 'tg.component/common/form/fields/TextField';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { Validation } from 'tg.constants/GlobalValidationSchema';

import { TaskDatePicker } from './TaskDatePicker';
import { AssigneeSearchSelect } from './assigneeSelect/AssigneeSearchSelect';
import { TaskLabel } from './TaskLabel';
import { messageService } from 'tg.service/MessageService';
import { TaskInfoItem } from './TaskInfoItem';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { TaskScope } from './TaskScope';
import { UserAccount } from 'tg.component/UserAccount';
import { getTaskRedirect, useTaskReport } from './utils';
import { Scope } from 'tg.fixtures/permissions';
import { ProjectWithAvatar } from 'tg.component/ProjectWithAvatar';

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
  taskNumber: number;
  onClose: () => void;
  projectId: number;
  projectScopes?: Scope[];
};

export const TaskDetail = ({ onClose, projectId, taskNumber }: Props) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();

  const taskLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}',
    method: 'get',
    path: { projectId, taskNumber },
  });

  const perUserReportLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}/per-user-report',
    method: 'get',
    path: { projectId, taskNumber },
  });

  const updateLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}',
    method: 'put',
    invalidatePrefix: ['/v2/projects/{projectId}/tasks', '/v2/user-tasks'],
  });

  const projectLoadable = useApiQuery({
    url: '/v2/projects/{projectId}',
    method: 'get',
    path: { projectId },
  });

  const scopes = projectLoadable.data?.computedPermission.scopes ?? [];
  const project = projectLoadable.data;

  const canEditTask = scopes.includes('tasks.edit');

  const { downloadReport } = useTaskReport();

  const data = taskLoadable.data;

  return (
    <Box data-cy="task-detail">
      <StyledMainTitle>
        <T keyName="task_detail_title" />
      </StyledMainTitle>
      {!data ? null : (
        <>
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
            onSubmit={(values) => {
              updateLoadable.mutate(
                {
                  path: { projectId, taskNumber },
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
                }
              );
            }}
          >
            {({ values, setFieldValue, isSubmitting, dirty, submitForm }) => (
              <StyledContainer>
                <StyledTopPart>
                  <TextField
                    name="name"
                    label={t('task_detail_field_name')}
                    data-cy="task-detail-field-name"
                    fullWidth
                    disabled={!canEditTask}
                  />
                  <AssigneeSearchSelect
                    label={t('task_detail_field_assignees')}
                    value={values.assignees}
                    onChange={(value) => setFieldValue('assignees', value)}
                    projectId={projectId}
                    disabled={!canEditTask}
                    filters={{
                      filterMinimalScope: 'TRANSLATIONS_VIEW',
                      filterViewLanguageId: data.language.id,
                    }}
                  />
                  <TaskDatePicker
                    value={values.dueDate ?? null}
                    onChange={(value) => setFieldValue('dueDate', value)}
                    label={t('task_detail_field_due_date')}
                    disabled={!canEditTask}
                  />
                </StyledTopPart>
                <TextField
                  label={t('task_detail_field_description')}
                  data-cy="task-detail-field-description"
                  name="description"
                  multiline
                  minRows={3}
                  disabled={!canEditTask}
                />

                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
                    {t('task_detail_scope_title')}
                  </Typography>
                  <Box display="flex" alignItems="center">
                    <Tooltip
                      title={t('task_link_translations_tooltip')}
                      disableInteractive
                    >
                      <IconButton
                        component={Link}
                        to={
                          project ? getTaskRedirect(project, data.number) : ''
                        }
                      >
                        <Translate01 />
                      </IconButton>
                    </Tooltip>
                    <Tooltip
                      title={t('task_detail_summarize_tooltip')}
                      disableInteractive
                    >
                      <IconButton
                        data-cy="task-detail-download-report"
                        onClick={() => downloadReport(projectId, data)}
                      >
                        <File06 />
                      </IconButton>
                    </Tooltip>
                  </Box>
                </Box>

                <TaskScope
                  task={data}
                  perUserData={perUserReportLoadable.data}
                />

                <Box
                  display="grid"
                  gridTemplateColumns="repeat(4, 1fr)"
                  pt="20px"
                >
                  <TaskInfoItem
                    label={t('task_detail_author_label')}
                    value={data.author && <UserAccount user={data.author} />}
                    data-cy="task-detail-author"
                  />
                  <TaskInfoItem
                    label={t('task_detail_created_at_label')}
                    value={formatDate(data.createdAt)}
                    data-cy="task-detail-created-at"
                  />
                  <TaskInfoItem
                    label={t('task_detail_closed_at_label')}
                    value={data.closedAt ? formatDate(data.closedAt) : null}
                    data-cy="task-detail-closed-at"
                  />
                  <TaskInfoItem
                    label={t('task_detail_project_label')}
                    value={project && <ProjectWithAvatar project={project} />}
                    data-cy="task-detail-project"
                  />
                </Box>

                <StyledActions>
                  <Button onClick={onClose}>{t('global_cancel_button')}</Button>
                  <LoadingButton
                    color="primary"
                    variant="contained"
                    loading={isSubmitting}
                    disabled={!dirty}
                    type="submit"
                    data-cy="task-detail-submit"
                    onClick={() => submitForm()}
                  >
                    {t('task_detail_submit_button')}
                  </LoadingButton>
                </StyledActions>
              </StyledContainer>
            )}
          </Formik>
        </>
      )}
    </Box>
  );
};
