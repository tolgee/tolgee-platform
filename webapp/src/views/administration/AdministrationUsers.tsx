import { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Chip, ListItem, ListItemText, styled } from '@mui/material';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { LINKS } from 'tg.constants/links';
import { DebugCustomerAccountButton } from './components/DebugCustomerAccountButton';
import { RoleSelector } from './components/RoleSelector';
import { BaseAdministrationView } from './components/BaseAdministrationView';
import { OptionsButton } from './components/OptionsButton';
import { MfaBadge } from '@tginternal/library/components/MfaBadge';

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  }
`;

export const AdministrationUsers = ({
  search,
  setSearch,
}: {
  search: string;
  setSearch: (str: string) => void;
}) => {
  const [page, setPage] = useState(0);

  const listPermitted = useApiQuery({
    url: '/v2/administration/users',
    method: 'get',
    query: {
      page,
      size: 20,
      search,
      sort: ['name,asc'],
    },
    options: {
      keepPreviousData: true,
    },
  });

  const { t } = useTranslate();

  return (
    <StyledWrapper>
      <DashboardPage>
        <BaseAdministrationView
          windowTitle={t('administration_users')}
          navigation={[
            [t('administration_users'), LINKS.ADMINISTRATION_USERS.build()],
          ]}
          initialSearch={search}
          allCentered
          hideChildrenOnLoading={false}
          loading={listPermitted.isFetching}
        >
          <PaginatedHateoasList
            onSearchChange={setSearch}
            onPageChange={setPage}
            searchText={search}
            loadable={listPermitted}
            renderItem={(u) => (
              <ListItem
                data-cy="administration-users-list-item"
                sx={{ display: 'grid', gridTemplateColumns: '1fr auto' }}
              >
                <ListItemText>
                  {u.name} | {u.username} <Chip size="small" label={u.id} />
                </ListItemText>
                <Box display="flex" justifyContent="center" gap={1}>
                  <MfaBadge enabled={u.mfaEnabled} />
                  <DebugCustomerAccountButton userId={u.id} />
                  <RoleSelector
                    user={u}
                    onSuccess={() => listPermitted.refetch()}
                  />
                  <OptionsButton user={u} />
                </Box>
              </ListItem>
            )}
          />
        </BaseAdministrationView>
      </DashboardPage>
    </StyledWrapper>
  );
};
