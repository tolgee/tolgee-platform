import { BaseView } from 'tg.component/layout/BaseView';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { Box, Button } from '@material-ui/core';
import { Link } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { T } from '@tolgee/react';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import React, { useState } from 'react';
import { useProject } from 'tg.hooks/useProject';
import { ActivityItem } from './ActivityItem';

export const ActivityPreview = () => {
  const project = useProject();
  const [page, setPage] = useState(0);

  const data = useApiQuery({
    url: '/v2/projects/{projectId}/activity',
    method: 'get',
    path: {
      projectId: project.id,
    },
    query: {
      page,
      size: 10,
      sort: ['id,desc'],
    },
  });

  return (
    <BaseView>
      <PaginatedHateoasList
        wrapperComponent={Box}
        onPageChange={setPage}
        loadable={data}
        renderItem={(r) => <ActivityItem key={r.revisionId} item={r} />}
        emptyPlaceholder={
          <EmptyListMessage
            loading={data.isFetching}
            hint={
              <Button
                component={Link}
                to={LINKS.PROJECT_ADD.build()}
                color="primary"
              >
                <T>projects_empty_action</T>
              </Button>
            }
          >
            <T>projects_empty</T>
          </EmptyListMessage>
        }
      />
    </BaseView>
  );
};
