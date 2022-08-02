import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { BaseView } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import {
  Box,
  Button,
  ListItem,
  ListItemSecondaryAction,
  MenuItem,
  Select,
  styled,
} from '@mui/material';
import ListItemText from '@mui/material/ListItemText';
import { AdministrationNav } from './AdministrationNav';
import { components } from 'tg.service/apiSchema.generated';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useUser } from 'tg.globalContext/helpers';
import { globalActions } from 'tg.store/global/GlobalActions';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { confirmation } from 'tg.hooks/confirmation';

type Role = components['schemas']['UserAccountModel']['globalServerRole'];

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1.main};
  }
`;

export const AdministrationUsers = () => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  const history = useHistory();

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

  const debugAccount = useApiMutation({
    url: '/v2/administration/users/{userId}/generate-token',
    method: 'get',
  });

  const setRoleMutation = useApiMutation({
    url: '/v2/administration/users/{userId}/set-role/{role}',
    method: 'put',
  });

  const t = useTranslate();
  const message = useMessage();
  const currentUser = useUser();

  const setRole = (userId: number, role: Role) => {
    confirmation({
      onConfirm() {
        setRoleMutation.mutate(
          {
            path: {
              role: role,
              userId: userId,
            },
          },
          {
            onSuccess: () => {
              message.success(<T>administration_role_set_success</T>);
              listPermitted.refetch();
            },
          }
        );
      },
    });
  };

  return (
    <StyledWrapper>
      <DashboardPage>
        <BaseView
          windowTitle={t('administration_users')}
          onSearch={setSearch}
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
                    <Button
                      data-cy="administration-user-debug-account"
                      size="small"
                      variant="contained"
                      color="error"
                      onClick={() => {
                        debugAccount.mutate(
                          { path: { userId: u.id } },
                          {
                            onSuccess: (r) => {
                              globalActions.debugCustomerAccount.dispatch(r);
                              history.push(LINKS.PROJECTS.build());
                            },
                          }
                        );
                      }}
                    >
                      <T>administration_user_debug</T>
                    </Button>
                    <Box display="flex" ml={1}>
                      <Select
                        data-cy="administration-user-role-select"
                        disabled={currentUser?.id === u.id}
                        size="small"
                        value={u.globalServerRole}
                        onChange={(e) => setRole(u.id, e.target.value as any)}
                      >
                        <MenuItem value={'USER'}>
                          <T>administration_user_role_user</T>
                        </MenuItem>
                        <MenuItem value={'ADMIN'}>
                          <T>administration_user_role_admin</T>
                        </MenuItem>
                      </Select>
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
