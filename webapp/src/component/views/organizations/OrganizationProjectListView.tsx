import { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { BaseView } from '../../layout/BaseView';
import ProjectListItem from '../projects/ProjectListItem';
import { useOrganization } from './useOrganization';
import { PaginatedHateoasList } from '../../common/list/PaginatedHateoasList';
import Box from '@material-ui/core/Box';
import { FabAddButtonLink } from '../../common/buttons/FabAddButtonLink';
import { LINKS } from '../../../constants/links';
import { useGetOrganizationProjects } from '../../../service/hooks/Organization';

export const OrganizationsProjectListView = () => {
  const t = useTranslate();

  const organization = useOrganization();

  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const loadable = useGetOrganizationProjects(
    organization!.slug,
    {
      page,
      search,
    },
    { keepPreviousData: true }
  );

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
