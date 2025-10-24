import { useState } from 'react';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useTranslate } from '@tolgee/react';
import {
  Box,
  Chip,
  ListItem,
  Typography,
  styled,
  useTheme,
} from '@mui/material';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { LINKS } from 'tg.constants/links';
import { DebugCustomerAccountButton } from './components/DebugCustomerAccountButton';
import { RoleSelector } from './components/RoleSelector';
import { BaseAdministrationView } from './components/BaseAdministrationView';
import { OptionsButton } from './components/OptionsButton';
import { MfaBadge } from 'tg.component/MfaBadge';

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

  const formatDate = useDateFormatter();
  const theme = useTheme();
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
                <Box>
                  <Box>
                    <Typography variant="body1">
                      {u.name} | {u.username} <Chip size="small" label={u.id} />
                    </Typography>
                  </Box>
                  <Box>
                    <Typography
                      variant="body2"
                      color={theme.palette.text.secondary}
                      data-cy="administration-user-activity"
                    >
                      {!u.lastActivity
                        ? t('administration_user_no_activity')
                        : t('administration_user_last_activity', {
                            date: formatDate(new Date(u.lastActivity), {
                              dateStyle: 'long',
                              timeStyle: 'short',
                            }),
                          })}
                    </Typography>
                  </Box>
                </Box>
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
