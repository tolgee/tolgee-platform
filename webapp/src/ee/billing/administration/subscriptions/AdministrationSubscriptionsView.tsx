import React, { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, FormControlLabel, Checkbox, styled, Switch } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { LINKS } from 'tg.constants/links';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { AdministrationSubscriptionsListItem } from './components/AdministrationSubscriptionsListItem';
import { PaginatedHateoasTable } from 'tg.component/common/table/PaginatedHateoasTable';
import SearchField from 'tg.component/common/form/fields/SearchField';

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
  const [showDeleted, setShowDeleted] = useUrlSearchState('showDeleted');
  const [trialing, setTrialing] = useUrlSearchState('trialing');

  const filterDeleted = showDeleted === 'true' ? undefined : (false as const);

  const listPermitted = useBillingApiQuery({
    url: '/v2/administration/billing/organizations',
    method: 'get',
    query: {
      page,
      size: 20,
      search,
      trialing: trialing === 'true' || undefined,
      sort: ['id,desc'],
      filterDeleted,
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
          <Box
            display="flex"
            alignItems="center"
            justifyContent="flex-end"
            gap={2}
            mb={1}
          >
            <FormControlLabel
              data-cy="administration-subscriptions-trialing-filter"
              control={
                <Checkbox
                  checked={trialing === 'true'}
                  onChange={(e) => {
                    setTrialing(e.target.checked ? 'true' : '');
                    setPage(0);
                  }}
                />
              }
              label={t('administration_subscriptions_trialing_filter')}
            />
            <FormControlLabel
              control={
                <Switch
                  checked={showDeleted === 'true'}
                  onChange={(_, checked) => {
                    setPage(0);
                    setShowDeleted(checked ? 'true' : '');
                  }}
                  size="small"
                />
              }
              label={t(
                'administration_subscriptions_show_deleted',
                'Show deleted'
              )}
              sx={{ mr: 0 }}
            />
            <Box sx={{ width: { xs: '100%', sm: 300 } }}>
              <SearchField
                data-cy="global-list-search"
                fullWidth
                initial={search}
                onSearch={(value) => {
                  setPage(0);
                  setSearch(value);
                }}
                variant="outlined"
                size="small"
              />
            </Box>
          </Box>
          <PaginatedHateoasTable
            wrapperComponentProps={{ className: 'listWrapper' }}
            onPageChange={setPage}
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
