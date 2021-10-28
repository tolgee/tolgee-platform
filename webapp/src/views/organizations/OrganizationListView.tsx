import { useState } from 'react';
import { Button } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import ListItemText from '@material-ui/core/ListItemText';
import { T, useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { FabAddButtonLink } from 'tg.component/common/buttons/FabAddButtonLink';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { SimpleListItem } from 'tg.component/common/list/SimpleListItem';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { OrganizationRoleType } from 'tg.service/response.types';

import { BaseUserSettingsView } from '../userSettings/BaseUserSettingsView';
import { useLeaveOrganization } from './useLeaveOrganization';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';

export const OrganizationsListView = () => {
  const t = useTranslate();

  const leaveOrganization = useLeaveOrganization();

  const [page, setPage] = useState(0);

  const organizatationsLoadable = useApiQuery({
    url: '/v2/organizations',
    method: 'get',
    query: {
      sort: ['name'],
      size: 10,
      page,
      params: {
        filterCurrentUserOwner: false,
      },
    },
  });

  return (
    <BaseUserSettingsView
      title={t('organizations_title')}
      containerMaxWidth="lg"
      loading={organizatationsLoadable.isFetching}
      hideChildrenOnLoading={false}
    >
      <PaginatedHateoasList
        loadable={organizatationsLoadable}
        emptyPlaceholder={
          <EmptyListMessage loading={organizatationsLoadable.isFetching} />
        }
        onPageChange={setPage}
        renderItem={(item) => (
          <SimpleListItem
            button
            //@ts-ignore
            component={Link}
            key={item.id}
            to={LINKS.ORGANIZATION_PROJECTS.build({
              [PARAMS.ORGANIZATION_SLUG]: item.slug,
            })}
          >
            <ListItemText data-cy="global-list-item-text">
              {item.name}
            </ListItemText>
            <ListItemSecondaryAction>
              <Box mr={1} display="inline">
                <Button
                  variant="outlined"
                  size="small"
                  onClick={() => leaveOrganization(item.id!)}
                  data-cy={'leave-organization-button'}
                >
                  <T>organization_users_leave</T>
                </Button>
              </Box>
              {item.currentUserRole == OrganizationRoleType.OWNER && (
                <Button
                  data-cy={'organization-settings-button'}
                  variant="outlined"
                  component={Link}
                  size="small"
                  to={LINKS.ORGANIZATION_PROFILE.build({
                    [PARAMS.ORGANIZATION_SLUG]: item.slug,
                  })}
                >
                  <T>organization_settings_button</T>
                </Button>
              )}
            </ListItemSecondaryAction>
          </SimpleListItem>
        )}
      />
      <Box
        display="flex"
        flexDirection="column"
        alignItems="flex-end"
        mt={2}
        pr={2}
      >
        <FabAddButtonLink to={LINKS.ORGANIZATIONS_ADD.build()} />
      </Box>
    </BaseUserSettingsView>
  );
};
