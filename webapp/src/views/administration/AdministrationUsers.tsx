import { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { BaseView } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { Box, ListItem, ListItemSecondaryAction, styled } from '@mui/material';
import ListItemText from '@mui/material/ListItemText';
import { AdministrationNav } from './AdministrationNav';
import { DebugCustomerAccountButton } from './DebugCustomerAccountButton';
import { RoleSelector } from './RoleSelector';
import { DeleteUserButton } from './DeleteUserButton';

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1.main};
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
        <BaseView
          windowTitle={t('administration_users')}
          onSearch={setSearch}
          initialSearch={search}
          containerMaxWidth="lg"
          allCentered
          hideChildrenOnLoading={false}
          loading={listPermitted.isFetching}
        >
          <AdministrationNav />
          <PaginatedHateoasList
            onPageChange={setPage}
            loadable={listPermitted}
            renderItem={(u) => (
              <ListItem data-cy="administration-users-list-item">
                <ListItemText>
                  {u.name} | {u.username}
                </ListItemText>
                <ListItemSecondaryAction>
                  <Box display="flex" justifyContent="center">
                    <DeleteUserButton user={u} />
                    <DebugCustomerAccountButton userId={u.id} />
                    <Box display="flex" ml={1}>
                      <RoleSelector
                        user={u}
                        onSuccess={() => listPermitted.refetch()}
                      />
                    </Box>
                  </Box>
                </ListItemSecondaryAction>
              </ListItem>
            )}
          />
        </BaseView>
      </DashboardPage>
    </StyledWrapper>
  );
};
