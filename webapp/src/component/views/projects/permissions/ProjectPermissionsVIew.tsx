import { FunctionComponent, useState } from 'react';
import { BaseView } from '../../../layout/BaseView';
import { T, useTranslate } from '@tolgee/react';
import { PaginatedHateoasList } from '../../../common/list/PaginatedHateoasList';
import { SimpleListItem } from '../../../common/list/SimpleListItem';
import { LINKS, PARAMS } from '../../../../constants/links';
import {
  Box,
  Chip,
  ListItemSecondaryAction,
  ListItemText,
  Typography,
} from '@material-ui/core';
import RevokePermissionsButton from './component/RevokePermissionsButton';
import { useProject } from '../../../../hooks/useProject';
import { translatedPermissionType } from '../../../../fixtures/translatePermissionFile';
import ProjectPermissionMenu from './component/ProjectPermissionMenu';
import { Navigation } from '../../../navigation/Navigation';
import { useApiQuery } from '../../../../service/http/useQueryApi';

export const ProjectPermissionsView: FunctionComponent = () => {
  const project = useProject();

  const t = useTranslate();

  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const listLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/users',
    method: 'get',
    path: { projectId: project.id },
    query: {
      pageable: {
        page,
        sort: ['name'],
      },
      search,
    },
    options: {
      keepPreviousData: true,
    },
  });

  const basePermissionText = translatedPermissionType(
    project.organizationOwnerBasePermissions!,
    true
  );

  return (
    <BaseView
      navigation={
        <Navigation
          path={[
            [
              project.name,
              LINKS.PROJECT_TRANSLATIONS.build({
                [PARAMS.PROJECT_ID]: project.id,
              }),
            ],
            [
              t('edit_project_permissions_title'),
              LINKS.PROJECT_TRANSLATIONS.build({
                [PARAMS.PROJECT_ID]: project.id,
              }),
            ],
          ]}
        />
      }
      containerMaxWidth="lg"
      loading={listLoadable.isFetching}
      hideChildrenOnLoading={false}
    >
      {project.organizationOwnerSlug && (
        <Box mb={2}>
          <Typography component={Box} alignItems={'center'} variant={'body1'}>
            <T>project_permission_information_text_base_permission_before</T>{' '}
            {basePermissionText}
          </Typography>

          <T>project_permission_information_text_base_permission_after</T>
        </Box>
      )}

      <PaginatedHateoasList
        loadable={listLoadable}
        onPageChange={setPage}
        onSearchChange={setSearch}
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
