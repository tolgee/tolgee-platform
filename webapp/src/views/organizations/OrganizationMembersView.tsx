import { FunctionComponent, useState } from 'react';
import { Box, Button, Grid, Theme, Typography } from '@material-ui/core';
import createStyles from '@material-ui/core/styles/createStyles';
import makeStyles from '@material-ui/core/styles/makeStyles';
import { T } from '@tolgee/react';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { SimpleListItem } from 'tg.component/common/list/SimpleListItem';
import { useUser } from 'tg.hooks/useUser';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { BaseOrganizationSettingsView } from './BaseOrganizationSettingsView';
import OrganizationRemoveUserButton from './components/OrganizationRemoveUserButton';
import { OrganizationRoleMenu } from './components/OrganizationRoleMenu';
import { useLeaveOrganization } from './useLeaveOrganization';
import { useOrganization } from './useOrganization';

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    container: {
      borderBottom: `1px solid ${theme.palette.grey.A100}`,
      '&:last-child': {
        borderBottom: `none`,
      },
    },
  })
);

export const OrganizationMembersView: FunctionComponent = () => {
  const organization = useOrganization();

  const classes = useStyles();

  const currentUser = useUser();

  const leaveOrganization = useLeaveOrganization();

  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const membersLoadable = useApiQuery({
    url: '/v2/organizations/{id}/users',
    method: 'get',
    path: { id: organization!.id },
    query: {
      page,
      sort: ['name'],
      size: 10,
      search,
    },
    options: {
      keepPreviousData: true,
    },
  });

  return (
    <BaseOrganizationSettingsView loading={membersLoadable.isFetching}>
      <PaginatedHateoasList
        loadable={membersLoadable}
        title={<T>organization_members_view_title</T>}
        onSearchChange={setSearch}
        onPageChange={setPage}
        renderItem={(i) => (
          <SimpleListItem className={classes.container} key={i.id}>
            <Grid container justify="space-between" alignItems="center">
              <Grid
                item
                data-cy={'organizations-user-name'}
                lg={3}
                md={3}
                sm={6}
              >
                <Box mr={1}>
                  <Typography variant={'body1'} noWrap>
                    {i.name}
                  </Typography>
                </Box>
              </Grid>
              <Grid
                item
                data-cy={'organizations-user-email'}
                lg={5}
                md={5}
                sm={6}
              >
                <Typography variant={'body1'} noWrap>
                  {i.username}
                </Typography>
              </Grid>
              <Grid
                item
                lg={4}
                md={4}
                style={{ display: 'flex', justifyContent: 'flex-end' }}
              >
                <OrganizationRoleMenu user={i} />
                <Box display={'inline'} ml={1}>
                  {currentUser?.id == i.id ? (
                    <Button
                      data-cy="organization-members-leave-button"
                      variant="outlined"
                      size="small"
                      aria-controls="simple-menu"
                      aria-haspopup="true"
                      onClick={() => leaveOrganization(organization!.id)}
                    >
                      <T>organization_users_leave</T>
                    </Button>
                  ) : (
                    <OrganizationRemoveUserButton
                      userId={i.id}
                      userName={i.name}
                    />
                  )}
                </Box>
              </Grid>
            </Grid>
          </SimpleListItem>
        )}
      />
    </BaseOrganizationSettingsView>
  );
};
