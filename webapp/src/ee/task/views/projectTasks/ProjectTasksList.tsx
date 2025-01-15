import { Box, ListProps, PaperProps, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { TaskFilterType, TaskItem } from 'tg.ee';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type TaskModel = components['schemas']['TaskModel'];

const StyledSeparator = styled('div')`
  grid-column: 1 / -1;
  height: 1px;
  background: ${({ theme }) => theme.palette.tokens.divider};
`;

type Props = {
  showAll: boolean;
  filter: TaskFilterType;
  onOpenDetail: (task: TaskModel) => void;
  search: string;
  newTaskActions: boolean;
};

export const ProjectTasksList = ({
  filter,
  search,
  onOpenDetail,
  newTaskActions,
}: Props) => {
  const project = useProject();
  const { t } = useTranslate();
  const [page, setPage] = useUrlSearchState('page', { defaultVal: '0' });
  const { isEnabled } = useEnabledFeatures();
  const tasksFeature = isEnabled('TASKS');

  const tasksLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 20,
      page: Number(page),
      search,
      sort: ['number,desc'],
      filterAssignee: filter.assignees,
      filterLanguage: filter.languages,
      filterType: filter.types,
      filterNotClosedBefore: filter.filterNotClosedBefore,
      filterAgency: filter.agencies,
    },
    options: {
      keepPreviousData: true,
    },
  });

  const allReady = tasksLoadable.isFetched;
  const allEmpty = tasksLoadable.data?.page?.totalElements === 0;

  if (allReady && allEmpty && !tasksFeature) {
    return (
      <Box>
        <DisabledFeatureBanner customMessage={t('tasks_feature_description')} />
      </Box>
    );
  }

  return (
    <PaginatedHateoasList
      loadable={tasksLoadable}
      onPageChange={(val) => setPage(String(val))}
      listComponentProps={
        {
          sx: {
            display: 'grid',
            gridTemplateColumns:
              '1fr minmax(15%, max-content) minmax(25%, max-content) minmax(10%, max-content) auto',
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
          newTaskActions={newTaskActions}
          task={task}
          onDetailOpen={(task) => onOpenDetail(task)}
          project={project}
          projectScopes={project.computedPermission.scopes}
        />
      )}
      itemSeparator={() => <StyledSeparator />}
    />
  );
};
