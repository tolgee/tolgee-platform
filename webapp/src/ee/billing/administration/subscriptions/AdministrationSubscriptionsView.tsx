import React, { useState } from 'react';
import { useHistory } from 'react-router-dom';
import { useTranslate } from '@tolgee/react';
import {
  Box,
  Chip,
  Link,
  ListItem,
  ListItemSecondaryAction,
  ListItemText,
  styled,
  Tooltip,
} from '@mui/material';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import Toolbar from '@mui/material/Toolbar';
import { AdministrationSubscriptionsCloudPlan } from './components/AdministrationSubscriptionsCloudPlan';
import { AdministrationSubscriptionsListItem } from './components/AdministrationSubscriptionsListItem';

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
          <PaginatedHateoasList
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
