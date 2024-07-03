import { useState } from 'react';
import { Home } from '@mui/icons-material';
import {
  Box,
  Dialog,
  ListProps,
  PaperProps,
  styled,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { BaseView } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { TaskItem } from 'tg.component/task/TaskItem';
import { components } from 'tg.service/apiSchema.generated';
import { TaskDetail } from 'tg.component/task/TaskDetail';

type TaskWithProjectModel = components['schemas']['TaskWithProjectModel'];

const StyledSeparator = styled('div')`
  grid-column: 1 / -1;
  height: 1px;
  background: ${({ theme }) => theme.palette.tokens.divider};
`;

export const MyTasksView = () => {
  const { t } = useTranslate();
  const [page, setPage] = useState(0);
  const [detail, setDetail] = useState<TaskWithProjectModel>();

  function handleDetailClose() {
    setDetail(undefined);
  }

  const tasksLoadable = useApiQuery({
    url: '/v2/user-tasks',
    method: 'get',
    query: {
      size: 20,
      page,
    },
    options: {
      keepPreviousData: true,
    },
  });

  return (
    <DashboardPage>
      <BaseView
        windowTitle={t('my_tasks_title')}
        maxWidth={800}
        navigation={[
          [null, LINKS.ROOT.build(), <Home key={0} fontSize="small" />],
          [t('my_tasks_title'), LINKS.MY_TASKS.build()],
        ]}
      >
        <Box sx={{ mt: '20px', mb: 2, display: 'flex' }}>
          <Typography variant="h6">{t('my_tasks_title')}</Typography>
        </Box>
        <PaginatedHateoasList
          loadable={tasksLoadable}
          onPageChange={setPage}
          listComponentProps={
            {
              sx: {
                display: 'grid',
                gridTemplateColumns: '3fr 1fr 2fr 60px 1fr auto',
                alignItems: 'center',
              },
            } as ListProps
          }
          wrapperComponentProps={
            {
              sx: {
                border: 'none',
                background: 'none',
              },
            } as PaperProps
          }
          renderItem={(task) => (
            <TaskItem
              task={task}
              project={task.project}
              onDetailOpen={(task) => {
                setDetail(task as TaskWithProjectModel);
              }}
              showProject={true}
            />
          )}
          itemSeparator={() => <StyledSeparator />}
        />
      </BaseView>
      {detail !== undefined && (
        <Dialog open={true} onClose={handleDetailClose} maxWidth="xl">
          <TaskDetail
            task={detail}
            onClose={handleDetailClose}
            project={detail.project}
          />
        </Dialog>
      )}
    </DashboardPage>
  );
};
