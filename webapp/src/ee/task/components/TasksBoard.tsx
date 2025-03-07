import { Box, styled, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { LoadingButton } from '@mui/lab';

import { components } from 'tg.service/apiSchema.generated';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';

import { useProjectBoardTasks } from '../views/projectTasks/useProjectBoardTasks';
import { BoardColumn } from './BoardColumn';
import { LabelHint } from 'tg.component/common/LabelHint';

type TaskModel = components['schemas']['TaskModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledColumns = styled(Box)`
  padding-top: 12px;
  display: flex;
  gap: 16px;
  min-width: 900px;
`;

const StyledContainer = styled(Box)`
  display: grid;
  overflow: auto;
`;

type TasksLoadable = ReturnType<typeof useProjectBoardTasks>;

type Props = {
  showAll: boolean;
  onOpenDetail: (task: TaskModel) => void;
  newTasks: TasksLoadable;
  inProgressTasks: TasksLoadable;
  doneTasks: TasksLoadable;
  project?: SimpleProjectModel;
  newTaskActions: boolean;
};

export const TasksBoard = ({
  showAll,
  onOpenDetail,
  newTasks,
  inProgressTasks,
  doneTasks,
  project,
  newTaskActions,
}: Props) => {
  const theme = useTheme();
  const { t } = useTranslate();
  const { isEnabled } = useEnabledFeatures();

  const tasksFeature = isEnabled('TASKS');

  const canFetchMore =
    newTasks.hasNextPage ||
    inProgressTasks.hasNextPage ||
    doneTasks.hasNextPage;

  function handleFetchMore() {
    newTasks.hasNextPage && newTasks.fetchNextPage();
    inProgressTasks.hasNextPage && inProgressTasks.fetchNextPage();
    doneTasks.hasNextPage && doneTasks.fetchNextPage();
  }
  const loadables = [newTasks, inProgressTasks, doneTasks];

  const isLoading = loadables.some((l) => l.isLoading);
  const isFetching = loadables.some((l) => l.isFetching);

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center">
        <BoxLoading />
      </Box>
    );
  }

  const allReady = loadables.every((l) => l.isFetched);
  const allEmpty = loadables.every(
    (l) => l.data?.pages?.[0].page?.totalElements === 0
  );

  if (allReady && allEmpty && !tasksFeature) {
    return (
      <Box>
        <DisabledFeatureBanner customMessage={t('tasks_feature_description')} />
      </Box>
    );
  }

  return (
    <StyledContainer>
      <StyledColumns>
        <BoardColumn
          state="NEW"
          tasks={newTasks.items}
          total={newTasks.data?.pages?.[0]?.page?.totalElements ?? 0}
          project={project}
          onDetailOpen={onOpenDetail}
          emptyMessage={t('task_board_empty_new')}
          newTaskActions={newTaskActions}
        />
        <BoardColumn
          state="IN_PROGRESS"
          tasks={inProgressTasks.items}
          total={inProgressTasks.data?.pages?.[0]?.page?.totalElements ?? 0}
          project={project}
          onDetailOpen={onOpenDetail}
          emptyMessage={t('task_board_empty_pending')}
          newTaskActions={newTaskActions}
        />
        <BoardColumn
          title={
            <Box display="inline" color={theme.palette.text.secondary}>
              <LabelHint title={t('task_board_closed_column_title_hint')}>
                <Box display="inline">
                  {t('task_board_closed_column_title')}
                </Box>
              </LabelHint>
              {!showAll && (
                <Box
                  display="inline"
                  textTransform="lowercase"
                  fontSize="12px"
                  color={theme.palette.text.secondary}
                >
                  {' '}
                  {t('task_board_last_30_days')}
                </Box>
              )}
            </Box>
          }
          tasks={doneTasks.items}
          total={doneTasks.data?.pages?.[0]?.page?.totalElements ?? 0}
          project={project}
          onDetailOpen={onOpenDetail}
          emptyMessage={t('task_board_empty_closed')}
          newTaskActions={newTaskActions}
        />
      </StyledColumns>
      <Box
        display="flex"
        justifyContent="center"
        gridColumn="1 / -1"
        paddingBottom={6}
        paddingTop={4}
        visibility={canFetchMore ? 'visible' : 'hidden'}
      >
        <LoadingButton loading={isFetching} onClick={handleFetchMore}>
          {t('global_load_more')}
        </LoadingButton>
      </Box>
    </StyledContainer>
  );
};
