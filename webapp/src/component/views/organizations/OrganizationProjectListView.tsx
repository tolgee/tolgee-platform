import * as React from 'react';
import { container } from 'tsyringe';
import { useTranslate } from '@tolgee/react';
import { OrganizationActions } from '../../../store/organization/OrganizationActions';
import { BaseView } from '../../layout/BaseView';
import ProjectListItem from '../projects/ProjectListItem';
import { useOrganization } from '../../../hooks/organizations/useOrganization';
import { SimplePaginatedHateoasList } from '../../common/list/SimplePaginatedHateoasList';
import Box from '@material-ui/core/Box';
import { FabAddButtonLink } from '../../common/buttons/FabAddButtonLink';
import { LINKS } from '../../../constants/links';

const actions = container.resolve(OrganizationActions);

export const OrganizationsProjectListView = () => {
  const t = useTranslate();

  const organization = useOrganization();

  const loadable = actions.useSelector((state) => state.loadables.listProjects);

  return (
    <BaseView
      title={t('organization_projects_title', { name: organization.name })}
      containerMaxWidth="md"
      hideChildrenOnLoading={false}
      loading={loadable.loading}
    >
      <SimplePaginatedHateoasList
        pageSize={20}
        dispatchParams={[{ path: { slug: organization.slug } }]}
        actions={actions}
        searchField
        loadableName="listProjects"
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
