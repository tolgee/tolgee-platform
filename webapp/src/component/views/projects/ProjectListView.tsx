import * as React from 'react';
import Box from '@material-ui/core/Box';
import {container} from 'tsyringe';
import {ProjectActions} from '../../../store/project/ProjectActions';
import {LINKS} from '../../../constants/links';
import {FabAddButtonLink} from '../../common/buttons/FabAddButtonLink';
import {BaseView} from '../../layout/BaseView';
import {PossibleProjectPage} from '../PossibleProjectPage';
import {useTranslate} from '@tolgee/react';
import {SimplePaginatedHateoasList} from '../../common/list/SimplePaginatedHateoasList';
import ProjectListItem from './ProjectListItem';

const actions = container.resolve(ProjectActions);

export const ProjectListView = () => {
  const listPermitted = actions.useSelector(
    (state) => state.loadables.listPermitted
  );

  const t = useTranslate();

  return (
    <PossibleProjectPage>
      <BaseView
        title={t('projects_title')}
        containerMaxWidth="md"
        hideChildrenOnLoading={false}
        loading={listPermitted.loading}
      >
        <SimplePaginatedHateoasList
          searchField
          pageSize={20}
          actions={actions}
          loadableName="listPermitted"
          renderItem={(r) => <ProjectListItem key={r.id} {...r} />}
        />
        <Box
          display="flex"
          flexDirection="column"
          alignItems="flex-end"
          mt={2}
          pr={2}
        >
          <FabAddButtonLink to={LINKS.REPOSITORY_ADD.build()} />
        </Box>
      </BaseView>
    </PossibleProjectPage>
  );
};
