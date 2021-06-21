import { useState } from 'react';
import Box from '@material-ui/core/Box';
import { LINKS } from '../../../constants/links';
import { FabAddButtonLink } from '../../common/buttons/FabAddButtonLink';
import { BaseView } from '../../layout/BaseView';
import { DashboardPage } from '../../layout/DashboardPage';
import { useTranslate } from '@tolgee/react';
import { PaginatedHateoasList } from '../../common/list/PaginatedHateoasList';
import ProjectListItem from './ProjectListItem';
import { useApiQuery } from '../../../service/http/useQueryApi';

export const ProjectListView = () => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  const listPermitted = useApiQuery({
    url: '/v2/projects',
    method: 'get',
    query: {
      pageable: {
        page,
        size: 20,
      },
      search,
    },
  });

  const t = useTranslate();

  return (
    <DashboardPage>
      <BaseView
        title={t('projects_title')}
        containerMaxWidth="md"
        hideChildrenOnLoading={false}
        loading={listPermitted.isLoading}
      >
        <PaginatedHateoasList
          onPageChange={setPage}
          onSearchChange={setSearch}
          loadable={listPermitted}
          renderItem={(r) => <ProjectListItem key={r.id} {...r} />}
        />
        <Box
          display="flex"
          flexDirection="column"
          alignItems="flex-end"
          mt={2}
          pr={2}
        >
          <FabAddButtonLink to={LINKS.PROJECT_ADD.build()} />
        </Box>
      </BaseView>
    </DashboardPage>
  );
};
