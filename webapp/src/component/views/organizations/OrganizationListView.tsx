import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { PaginatedHateoasList } from '../../common/list/PaginatedHateoasList';
import { LINKS, PARAMS } from '../../../constants/links';
import ListItemText from '@material-ui/core/ListItemText';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import { Link } from 'react-router-dom';
import { OrganizationRoleType } from '../../../service/response.types';
import { FabAddButtonLink } from '../../common/buttons/FabAddButtonLink';
import Box from '@material-ui/core/Box';
import { BaseUserSettingsView } from '../userSettings/BaseUserSettingsView';
import { SimpleListItem } from '../../common/list/SimpleListItem';
import { Button } from '@material-ui/core';
import { useLeaveOrganization } from './useLeaveOrganization';
import { useGetOrganizations } from '../../../service/hooks/Organization';

export const OrganizationsListView = () => {
  const t = useTranslate();

  const leaveOrganization = useLeaveOrganization();

  const [page, setPage] = useState(0);

  const organizatationsLoadable = useGetOrganizations({
    page,
    filterCurrentUserOwner: false,
    sort: ['name'],
    size: 10,
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
