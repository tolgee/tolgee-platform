import React, { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { styled } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { LINKS } from 'tg.constants/links';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { AdministrationSubscriptionsListItem } from './components/AdministrationSubscriptionsListItem';
import { PaginatedHateoasTable } from 'tg.component/common/table/PaginatedHateoasTable';

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  }
`;

export const AdministrationSubscriptionsView = () => {
  const [page, setPage] = useState(0);

  const [search, setSearch] = useUrlSearchState('search');

  const listPermitted = useBillingApiQuery({
    url: '/v2/administration/billing/organizations',
    method: 'get',
    query: {
      page,
      size: 20,
      search,
      sort: ['id,desc'],
    },
  });

  const { t } = useTranslate();

  return (
    <StyledWrapper>
      <DashboardPage>
        <BaseAdministrationView
          windowTitle={t('administration_subscriptions')}
          navigation={[
            [
              t('administration_organizations'),
              LINKS.ADMINISTRATION_ORGANIZATIONS.build(),
            ],
          ]}
          initialSearch={search}
          allCentered
          hideChildrenOnLoading={false}
          loading={listPermitted.isFetching}
        >
          <PaginatedHateoasTable
            wrapperComponentProps={{ className: 'listWrapper' }}
            onPageChange={setPage}
            onSearchChange={setSearch}
            searchText={search}
            loadable={listPermitted}
            renderItem={(item) => (
              <AdministrationSubscriptionsListItem item={item} />
            )}
          />
        </BaseAdministrationView>
      </DashboardPage>
    </StyledWrapper>
  );
};
