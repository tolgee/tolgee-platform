import {default as React, FunctionComponent} from 'react';
import {BaseView} from '../../../layout/BaseView';
import {useSelector} from 'react-redux';
import {AppState} from '../../../../store';
import {container} from 'tsyringe';
import {T} from '@tolgee/react';
import {SimplePaginatedHateoasList} from '../../../common/list/SimplePaginatedHateoasList';
import {ProjectActions} from '../../../../store/project/ProjectActions';
import {SimpleListItem} from '../../../common/list/SimpleListItem';
import {Box, Chip, ListItemSecondaryAction, ListItemText, Typography,} from '@material-ui/core';
import RevokePermissionsButton from './component/RevokePermissionsButton';
import {useProject} from '../../../../hooks/useProject';
import {translatedPermissionType} from '../../../../fixtures/translatePermissionFile';
import ProjectPermissionMenu from './component/ProjectPermissionMenu';

const projectActions = container.resolve(ProjectActions);

export const ProjectPermissionsView: FunctionComponent = () => {
  const project = useProject();

  let listLoadable = useSelector(
    (state: AppState) => state.projects.loadables.listUsersForPermissions
  );

  const basePermissionText = translatedPermissionType(
    project.organizationOwnerBasePermissions!,
    true
  );

  return (
    <BaseView
      title={<T>edit_project_permissions_title</T>}
      containerMaxWidth="lg"
      loading={listLoadable.loading}
      hideChildrenOnLoading={false}
    >
      {project.organizationOwnerAddressPart && (
        <Box mb={2}>
          <Typography component={Box} alignItems={'center'} variant={'body1'}>
            <T>project_permission_information_text_base_permission_before</T>{' '}
            {basePermissionText}
          </Typography>

          <T>project_permission_information_text_base_permission_after</T>
        </Box>
      )}

      <SimplePaginatedHateoasList
        searchField
        actions={projectActions}
        dispatchParams={[{ path: { projectId: project.id } }]}
        loadableName="listUsersForPermissions"
        renderItem={(u) => (
          <SimpleListItem>
            <ListItemText>
              {u.name} ({u.username}){' '}
              {u.organizationRole && (
                <Chip size="small" label={project.organizationOwnerName} />
              )}
            </ListItemText>
            <ListItemSecondaryAction>
              <Box mr={1} display="inline">
                <ProjectPermissionMenu user={u} />
              </Box>
              <RevokePermissionsButton user={u} />
            </ListItemSecondaryAction>
          </SimpleListItem>
        )}
      />
    </BaseView>
  );
};
