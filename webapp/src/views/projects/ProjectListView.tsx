import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';

import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { BaseView } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import DashboardProjectListItem from 'tg.views/projects/DashboardProjectListItem';
import { Box, Button, styled } from '@mui/material';
import { Link } from 'react-router-dom';
import {
  useConfig,
  useCurrentOrganization,
} from 'tg.hooks/InitialDataProvider';
import { OrganizationSwitch } from 'tg.component/OrganizationSwitch';
import { Usage } from './dashboard/Usage';

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1.main};
  }
`;

const StyledUsage = styled(Box)`
  display: flex;
  flex-direction: column;
  justify-content: center;
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
  width: 100%;
  max-width: 200px;
`;

export const ProjectListView = () => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const config = useConfig();
  const organization = useCurrentOrganization();

  const listPermitted = useApiQuery({
    url: '/v2/organizations/{slug}/projects-with-stats',
    method: 'get',
    path: { slug: organization.slug || '' },
    query: {
      page,
      size: 20,
      search,
      sort: ['id,desc'],
    },
    options: {
      keepPreviousData: true,
      enabled: Boolean(organization.slug),
    },
  });

  const t = useTranslate();

  const showStats =
    config.billing.enabled && organization.currentUserRole === 'OWNER';

  return (
    <StyledWrapper>
      <DashboardPage>
        <BaseView
          title={<T>projects_title</T>}
          windowTitle={t('projects_title')}
          onSearch={setSearch}
          containerMaxWidth="lg"
          addLinkTo={
            organization.currentUserRole === 'OWNER'
              ? LINKS.PROJECT_ADD.build()
              : undefined
          }
          hideChildrenOnLoading={false}
          navigation={[[<OrganizationSwitch key={0} />]]}
          navigationRight={
            showStats &&
            organization && (
              <StyledUsage>
                <Usage
                  organizationId={organization.id}
                  slug={organization.slug}
                />
              </StyledUsage>
            )
          }
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
                  <Button
                    component={Link}
                    to={LINKS.PROJECT_ADD.build()}
                    color="primary"
                  >
                    <T>projects_empty_action</T>
                  </Button>
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
