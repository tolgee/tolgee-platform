import { FunctionComponent, useEffect, useState } from 'react';
import { Box, Button, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { LINKS, PARAMS } from 'tg.constants/links';
import { usePermissionTranslation } from 'tg.translationTools/usePermissionTranslation';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { MemberItem } from './component/MemberItem';
import { InviteDialog } from './component/InviteDialog';
import { InvitationItem } from './component/InvitationItem';
import { BaseProjectView } from '../BaseProjectView';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useReportEvent } from 'tg.hooks/useReportEvent';

export const ProjectMembersView: FunctionComponent = () => {
  const project = useProject();

  const { t } = useTranslate();

  const [inviteOpen, setInviteOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const membersLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/users',
    method: 'get',
    path: { projectId: project.id },
    query: {
      page,
      sort: ['name'],
      search,
    },
    options: {
      keepPreviousData: true,
    },
  });

  const invitationsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/invitations',
    method: 'get',
    path: { projectId: project.id },
    options: {
      keepPreviousData: true,
    },
  });

  const { satisfiesPermission } = useProjectPermissions();

  const canEditMembers = satisfiesPermission('members.edit');

  const translatePermission = usePermissionTranslation();

  useGlobalLoading(invitationsLoadable.isFetching);

  const reportEvent = useReportEvent();

  useEffect(() => {
    reportEvent('PROJECT_MEMBERS_VIEW');
  }, []);

  return (
    <BaseProjectView
      windowTitle={t('project_members_title')}
      navigation={[
        [
          t('project_members_title'),
          LINKS.PROJECT_PERMISSIONS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
      lg={9}
      md={12}
      containerMaxWidth="lg"
      loading={membersLoadable.isFetching}
      hideChildrenOnLoading={false}
    >
      {project.organizationOwner?.slug && (
        <Box mb={2}>
          <Typography component={Box} alignItems={'center'} variant={'body1'}>
            <T keyName="project_permission_information_text_base_permission_before" />{' '}
            {translatePermission(
              project.organizationOwner.basePermissions.type!
            )}
          </Typography>

          <T keyName="project_permission_information_text_base_permission_after" />
        </Box>
      )}
      <ProjectLanguagesProvider>
        <Box
          mb={1}
          display="flex"
          justifyContent="space-between"
          alignItems="center"
        >
          <Typography variant="h6">{t('invitations_title')}</Typography>
          {canEditMembers && (
            <Button
              color="primary"
              variant="contained"
              onClick={() => setInviteOpen(true)}
              data-cy="invite-generate-button"
            >
              {t('invitations_invite_button')}
            </Button>
          )}
        </Box>

        <PaginatedHateoasList
          loadable={invitationsLoadable}
          renderItem={(i) => <InvitationItem invitation={i} />}
          emptyPlaceholder={
            <Box m={4} display="flex" justifyContent="center">
              <Typography color="textSecondary">
                {t('invite_user_nothing_found')}
              </Typography>
            </Box>
          }
        />

        <InviteDialog onClose={() => setInviteOpen(false)} open={inviteOpen} />

        <Box mt={4} />

        <PaginatedHateoasList
          title={t('project_menu_members')}
          loadable={membersLoadable}
          onPageChange={setPage}
          onSearchChange={setSearch}
          emptyPlaceholder={
            <Box m={4} display="flex" justifyContent="center">
              <Typography color="textSecondary">
                {t('global_nothing_found')}
              </Typography>
            </Box>
          }
          renderItem={(u) => <MemberItem user={u} />}
        />
      </ProjectLanguagesProvider>
    </BaseProjectView>
  );
};
