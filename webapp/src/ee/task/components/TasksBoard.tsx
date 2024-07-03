import { Box, styled, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { LoadingButton } from '@mui/lab';

import { components } from 'tg.service/apiSchema.generated';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useTaskStateTranslation } from 'tg.translationTools/useTaskStateTranslation';

import { useProjectBoardTasks } from '../views/projectTasks/useProjectBoardTasks';
import { useStateColor } from './TaskState';
import { BoardColumn } from './BoardColumn';

type TaskModel = components['schemas']['TaskModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledColumns = styled(Box)`
  padding-top: 12px;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
  min-width: 800px;
`;

const StyledContainer = styled(Box)`
  display: grid;
  overflow: auto;
`;

type TasksLoadable = ReturnType<typeof useProjectBoardTasks>;

type Props = {
  showClosed: boolean;
  onOpenDetail: (task: TaskModel) => void;
  newTasks: TasksLoadable;
  inProgressTasks: TasksLoadable;
  doneTasks: TasksLoadable;
  project?: SimpleProjectModel;
};

export const TasksBoard = ({
  showClosed,
  onOpenDetail,
  newTasks,
  inProgressTasks,
  doneTasks,
  project,
}: Props) => {
  const theme = useTheme();
  const { t } = useTranslate();
  const translateState = useTaskStateTranslation();
  const stateColor = useStateColor();

  const canFetchMore =
    newTasks.hasNextPage ||
    inProgressTasks.hasNextPage ||
    doneTasks.hasNextPage;

  function handleFetchMore() {
    newTasks.fetchNextPage();
    inProgressTasks.fetchNextPage();
    doneTasks.fetchNextPage();
  }

  const isLoading =
    newTasks.isLoading || inProgressTasks.isLoading || doneTasks.isLoading;
  const isFetching =
    newTasks.isFetching || inProgressTasks.isFetching || doneTasks.isFetching;

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center">
        <BoxLoading />
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
        />
        <BoardColumn
          state="IN_PROGRESS"
          tasks={inProgressTasks.items}
          total={inProgressTasks.data?.pages?.[0]?.page?.totalElements ?? 0}
          project={project}
          onDetailOpen={onOpenDetail}
          emptyMessage={t('task_board_empty_pending')}
        />
        <BoardColumn
          state="DONE"
          title={
            showClosed ? (
              <Box>
                <Box display="inline" color={stateColor('DONE')}>
                  {translateState('DONE')}
                </Box>
                <Box display="inline" color={stateColor('CLOSED')}>
                  {' & '}
                  {translateState('CLOSED')}
                </Box>
              </Box>
            ) : (
              <Box>
                <Box display="inline" color={stateColor('DONE')}>
                  {translateState('DONE')}
                </Box>
                <Box
                  display="inline"
                  textTransform="lowercase"
                  fontSize="12px"
                  color={theme.palette.text.secondary}
                >
                  {' '}
                  {t('task_board_last_30_days')}
                </Box>
              </Box>
            )
          }
          tasks={doneTasks.items}
          total={doneTasks.data?.pages?.[0]?.page?.totalElements ?? 0}
          project={project}
          onDetailOpen={onOpenDetail}
          emptyMessage={t('task_board_empty_completed')}
        />
        {canFetchMore && (
          <Box
            display="flex"
            justifyContent="center"
            gridColumn="1 / -1"
            paddingBottom={4}
          >
            <LoadingButton loading={isFetching} onClick={handleFetchMore}>
              {t('global_load_more')}
            </LoadingButton>
          </Box>
        )}
      </StyledColumns>
    </StyledContainer>
  );
};
