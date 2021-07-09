import { useState } from 'react';
import Box from '@material-ui/core/Box';
import { useTranslate } from '@tolgee/react';

import { FabAddButtonLink } from 'tg.component/common/buttons/FabAddButtonLink';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { BaseView } from 'tg.component/layout/BaseView';
import { LINKS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import ProjectListItem from '../projects/ProjectListItem';
import { useOrganization } from './useOrganization';

export const OrganizationsProjectListView = () => {
  const t = useTranslate();

  const organization = useOrganization();

  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const loadable = useApiQuery({
    url: '/v2/organizations/{id}/projects',
    method: 'get',
    path: { id: organization!.id },
    query: {
      page,
      search,
    },
    options: {
      keepPreviousData: true,
    },
  });

  return (
    <BaseView
      title={t('organization_projects_title', { name: organization!.name })}
      containerMaxWidth="md"
      hideChildrenOnLoading={false}
      loading={loadable.isFetching}
    >
      <PaginatedHateoasList
        loadable={loadable}
        onSearchChange={setSearch}
        onPageChange={setPage}
        renderItem={(r) => <ProjectListItem {...r} />}
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
  );
};
