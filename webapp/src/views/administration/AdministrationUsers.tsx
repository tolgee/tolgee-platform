import { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import {
  Box,
  ListItem,
  ListItemSecondaryAction,
  ListItemText,
  styled,
} from '@mui/material';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { LINKS } from 'tg.constants/links';
import { DebugCustomerAccountButton } from './components/DebugCustomerAccountButton';
import { RoleSelector } from './components/RoleSelector';
import { DeleteUserButton } from './components/DeleteUserButton';
import { ToggleUserButton } from './components/ToggleUserButton';
import { BaseAdministrationView } from './components/BaseAdministrationView';

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
        <BaseAdministrationView
          windowTitle={t('administration_users')}
          navigation={[
            [t('administration_users'), LINKS.ADMINISTRATION_USERS.build()],
          ]}
          initialSearch={search}
          containerMaxWidth="lg"
          allCentered
          hideChildrenOnLoading={false}
          loading={listPermitted.isFetching}
        >
          <PaginatedHateoasList
            onSearchChange={setSearch}
            onPageChange={setPage}
            loadable={listPermitted}
            renderItem={(u) => (
              <ListItem data-cy="administration-users-list-item">
                <ListItemText>
                  {u.name} | {u.username}
                </ListItemText>
                <ListItemSecondaryAction>
                  <Box display="flex" justifyContent="center">
                    <ToggleUserButton user={u} />
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
        </BaseAdministrationView>
      </DashboardPage>
    </StyledWrapper>
  );
};
