import { useState } from 'react';
import { Formik } from 'formik';
import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  Button,
  DialogTitle,
  IconButton,
  styled,
  Typography,
} from '@mui/material';
import { Link } from 'react-router-dom';
import { DotsVertical } from '@untitled-ui/icons-react';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { TextField } from 'tg.component/common/form/fields/TextField';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { messageService } from 'tg.service/MessageService';
import { UserAccount } from 'tg.component/UserAccount';
import { ProjectWithAvatar } from 'tg.component/ProjectWithAvatar';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';

import { TaskDatePicker } from './TaskDatePicker';
import { AssigneeSearchSelect } from './assigneeSelect/AssigneeSearchSelect';
import { TaskLabel } from './TaskLabel';
import { TaskInfoItem } from './TaskInfoItem';
import { TaskScope } from './TaskScope';
import { getTaskRedirect } from './utils';
import { TaskMenu } from './TaskMenu';
import { BoxLoading } from 'tg.component/common/BoxLoading';

type TaskModel = components['schemas']['TaskModel'];

const StyledHeader = styled(Box)`
  display: grid;
  padding: ${({ theme }) => theme.spacing(0, 2, 2, 3)};
  grid-template-columns: 1fr 60px;
  grid-template-areas:
    'title    menu'
    'subtitle menu';
`;

const StyledMainTitle = styled(DialogTitle)`
  padding-left: 0px;
  padding-right: 0px;
  grid-area: title;
  padding-bottom: 0px;
`;

const StyledSubtitle = styled('div')`
  grid-area: subtitle;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledMenu = styled('div')`
  grid-area: menu;
  align-self: center;
  justify-self: end;
`;

const StyledContainer = styled('div')`
  display: grid;
  padding: ${({ theme }) => theme.spacing(3)};
  gap: ${({ theme }) => theme.spacing(0.5, 3)};
  padding-top: ${({ theme }) => theme.spacing(1)};
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
  task?: TaskModel;
  onClose: () => void;
  projectId: number;
};

export const TaskDetail = ({ onClose, projectId, taskNumber, task }: Props) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

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
    invalidatePrefix: [
      '/v2/projects/{projectId}/translations',
      '/v2/projects/{projectId}/tasks',
      '/v2/user-tasks',
    ],
  });

  const projectLoadable = useApiQuery({
    url: '/v2/projects/{projectId}',
    method: 'get',
    path: { projectId },
  });

  const scopes = projectLoadable.data?.computedPermission.scopes ?? [];
  const project = projectLoadable.data;

  const canEditTask = scopes.includes('tasks.edit');

  const handleClose = () => {
    setAnchorEl(null);
  };

  const data = taskLoadable.data ?? task;

  if (!data && taskLoadable.isLoading) {
    return (
      <Box display="grid" width="min(85vw, 800px)" justifyContent="center">
        <BoxLoading />
      </Box>
    );
  }

  return (
    <Box data-cy="task-detail" display="grid" width="min(85vw, 800px)">
      <StyledHeader>
        <StyledMainTitle>
          <T keyName="task_detail_title" />
        </StyledMainTitle>
        {data && projectLoadable.data && (
          <>
            <StyledSubtitle>
              <TaskLabel task={data} />
            </StyledSubtitle>
            <StyledMenu>
              <IconButton
                size="small"
                onClick={stopAndPrevent((e) => setAnchorEl(e.currentTarget))}
                data-cy="task-item-menu"
              >
                <DotsVertical />
              </IconButton>
              <TaskMenu
                task={data}
                anchorEl={anchorEl}
                onClose={handleClose}
                project={projectLoadable.data}
                projectScopes={projectLoadable.data.computedPermission.scopes}
                newTaskActions={false}
                hideTaskDetail={true}
              />
            </StyledMenu>
          </>
        )}
      </StyledHeader>
      {!data ? null : (
        <>
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

                <Box
                  sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    mt: 1,
                    mb: 1,
                  }}
                >
                  <Typography variant="subtitle2">
                    {t('task_detail_scope_title')}
                  </Typography>
                  <Button
                    color="primary"
                    component={Link}
                    to={project ? getTaskRedirect(project, data.number) : ''}
                  >
                    <T keyName="task_link_translations_tooltip" />
                  </Button>
                </Box>

                <TaskScope
                  task={data}
                  perUserData={perUserReportLoadable.data}
                  projectId={projectId}
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
                  <Button onClick={onClose}>{t('global_close_button')}</Button>
                  {canEditTask && (
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
                  )}
                </StyledActions>
              </StyledContainer>
            )}
          </Formik>
        </>
      )}
    </Box>
  );
};
