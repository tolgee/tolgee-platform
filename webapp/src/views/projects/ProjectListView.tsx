import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';

import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { BaseView } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import DashboardProjectListItem from 'tg.views/projects/DashboardProjectListItem';
import { Button, styled } from '@mui/material';
import { Link } from 'react-router-dom';
import { useIsAdmin, usePreferredOrganization } from 'tg.globalContext/helpers';
import { OrganizationSwitch } from 'tg.component/organizationSwitch/OrganizationSwitch';
import { Usage } from 'tg.component/billing/Usage';

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1.main};
  }
`;

export const ProjectListView = () => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const { preferredOrganization } = usePreferredOrganization();

  const listPermitted = useApiQuery({
    url: '/v2/organizations/{slug}/projects-with-stats',
    method: 'get',
    path: { slug: preferredOrganization?.slug || '' },
    query: {
      page,
      size: 20,
      search,
      sort: ['id,desc'],
    },
    options: {
      keepPreviousData: true,
      enabled: Boolean(preferredOrganization?.slug),
    },
  });

  const { t } = useTranslate();

  const isOrganizationOwner =
    preferredOrganization?.currentUserRole === 'OWNER';

  const isAdmin = useIsAdmin();

  const isAdminAccess = !preferredOrganization?.currentUserRole && isAdmin;

  return (
    <StyledWrapper>
      <DashboardPage isAdminAccess={isAdminAccess}>
        <BaseView
          windowTitle={t('projects_title')}
          onSearch={setSearch}
          containerMaxWidth="lg"
          allCentered
          addLinkTo={
            isOrganizationOwner || isAdminAccess
              ? LINKS.PROJECT_ADD.build()
              : undefined
          }
          hideChildrenOnLoading={false}
          navigation={[
            [<OrganizationSwitch key={0} />],
            [t('projects_title'), LINKS.PROJECTS.build()],
          ]}
          navigationRight={<Usage />}
          loading={listPermitted.isFetching}
        >
          <PaginatedHateoasList
            wrapperComponentProps={{ className: 'listWrapper' }}
            onPageChange={setPage}
            loadable={listPermitted}
            renderItem={(r) => <DashboardProjectListItem key={r.id} {...r} />}
            emptyPlaceholder={
              <EmptyListMessage
                loading={listPermitted.isFetching}
                hint={
                  isOrganizationOwner ? (
                    <Button
                      component={Link}
                      to={LINKS.PROJECT_ADD.build()}
                      color="primary"
                    >
                      <T>projects_empty_action</T>
                    </Button>
                  ) : undefined
                }
              >
                <T>projects_empty</T>
              </EmptyListMessage>
            }
          />
        </BaseView>
      </DashboardPage>
    </StyledWrapper>
  );
};
