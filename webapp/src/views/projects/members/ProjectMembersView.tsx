import { FunctionComponent, useEffect, useState } from 'react';
import { Box, Button, styled, Tab, Tabs, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useReportEvent } from 'tg.hooks/useReportEvent';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import SearchField from 'tg.component/common/form/fields/SearchField';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { AgencyFilter } from './component/AgencyFilter';
import { MemberItem } from './component/MemberItem';
import { ContributorItem } from './component/ContributorItem';
import { InviteDialog } from './component/InviteDialog';
import { InvitationItem } from './component/InvitationItem';
import { BaseProjectView } from '../BaseProjectView';
import { useConfig, useEnabledFeatures } from 'tg.globalContext/helpers';

const StyledTabs = styled(Tabs)`
  margin-bottom: -1px;
`;

const StyledTabWrapper = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  margin-bottom: ${({ theme }) => theme.spacing(2)};
`;

export const ProjectMembersView: FunctionComponent<
  React.PropsWithChildren<unknown>
> = () => {
  const project = useProject();
  const config = useConfig();
  const { isEnabled } = useEnabledFeatures();
  const showAgencyFilter =
    config.billing.enabled && isEnabled('ORDER_TRANSLATION');

  const { t } = useTranslate();

  const [activeTab, setActiveTab] = useUrlSearchState('tab', {
    defaultVal: 'team',
  });
  const [inviteOpen, setInviteOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [contributorsPage, setContributorsPage] = useState(0);
  const [filterAgency, setFilterAgency] = useUrlSearchState('agency', {
    array: true,
    defaultVal: [],
  });

  const membersLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/users',
    method: 'get',
    path: { projectId: project.id },
    query: {
      page,
      sort: ['name'],
      search,
      filterAgency: filterAgency?.map((a) => Number(a)),
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

  const contributorsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/contributors',
    method: 'get',
    path: { projectId: project.id },
    query: {
      page: contributorsPage,
    },
    options: {
      enabled: project.public,
      keepPreviousData: true,
    },
  });

  const hasContributors =
    (contributorsLoadable.data?.page?.totalElements ?? 0) > 0;
  const currentTab = activeTab === 'community' ? 'community' : 'team';
  const showTabs = project.public && hasContributors;
  const showCommunity = showTabs && currentTab === 'community';

  const { satisfiesPermission } = useProjectPermissions();

  const canEditMembers = satisfiesPermission('members.edit');

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
      maxWidth="normal"
      loading={invitationsLoadable.isLoading}
    >
      <Box display="grid">
        <ProjectLanguagesProvider>
          {showTabs && (
            <StyledTabWrapper>
              <StyledTabs
                value={currentTab}
                onChange={(_, value) => setActiveTab(value)}
              >
                <Tab
                  value="team"
                  label={t('project_members_tab_team', 'Team')}
                  data-cy="project-members-tab-team"
                />
                <Tab
                  value="community"
                  label={t('project_members_tab_community', 'Community')}
                  data-cy="project-members-tab-community"
                />
              </StyledTabs>
            </StyledTabWrapper>
          )}
          <Box display={showCommunity ? 'none' : 'grid'}>
            <QuickStartHighlight
              itemKey="invitations"
              message={t('quick_start_item_invitations_hint')}
              disabled={inviteOpen}
              borderRadius="5px"
              offset={10}
            >
              <Box>
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
              </Box>
            </QuickStartHighlight>

            <InviteDialog
              onClose={() => setInviteOpen(false)}
              open={inviteOpen}
            />

            <Box mt={4} />

            <QuickStartHighlight
              itemKey="members"
              message={t('quick_start_item_members_hint')}
              disabled={inviteOpen}
              borderRadius="5px"
              offset={10}
            >
              <Box>
                <Box
                  mb={1}
                  display="flex"
                  justifyContent="space-between"
                  alignItems="center"
                >
                  <Typography variant="h6">
                    {t('project_menu_members')}
                  </Typography>
                  <Box
                    display="flex"
                    gap={2}
                    justifyContent="end"
                    mb={1}
                    alignItems="stretch"
                  >
                    {showAgencyFilter && (
                      <AgencyFilter
                        value={filterAgency?.map((a) => Number(a))}
                        onChange={(value) =>
                          setFilterAgency(value.map((a) => String(a)))
                        }
                      />
                    )}
                    <SearchField onSearch={setSearch} />
                  </Box>
                </Box>
                <PaginatedHateoasList
                  loadable={membersLoadable}
                  onPageChange={setPage}
                  emptyPlaceholder={
                    <Box m={4} display="flex" justifyContent="center">
                      <Typography color="textSecondary">
                        {t('global_nothing_found')}
                      </Typography>
                    </Box>
                  }
                  renderItem={(u) => <MemberItem user={u} />}
                />
              </Box>
            </QuickStartHighlight>
          </Box>

          {showCommunity && (
            <PaginatedHateoasList
              loadable={contributorsLoadable}
              onPageChange={setContributorsPage}
              renderItem={(c) => <ContributorItem contributor={c} />}
            />
          )}
        </ProjectLanguagesProvider>
      </Box>
    </BaseProjectView>
  );
};
