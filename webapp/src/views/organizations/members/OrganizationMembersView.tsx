import { FunctionComponent, useEffect, useState } from 'react';
import { Box, Button, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { BaseOrganizationSettingsView } from '../components/BaseOrganizationSettingsView';
import { useOrganization } from '../useOrganization';
import { MemberItem } from './MemberItem';
import { SimpleList } from 'tg.component/common/list/SimpleList';
import { InviteDialog } from './InviteDialog';
import { InvitationItem } from './InvitationItem';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useReportEvent } from 'tg.hooks/useReportEvent';

export const OrganizationMembersView: FunctionComponent = () => {
  const organization = useOrganization();
  const { t } = useTranslate();

  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [inviteOpen, setInviteOpen] = useState(false);

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

  const invitationsLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/invitations',
    method: 'get',
    path: { organizationId: organization!.id },
    options: {
      keepPreviousData: true,
    },
  });

  const invitations =
    invitationsLoadable.data?._embedded?.organizationInvitations;

  const reportEvent = useReportEvent();

  useEffect(() => {
    reportEvent('ORGANIZATION_MEMBERS_VIEW');
  }, []);

  return (
    <BaseOrganizationSettingsView
      loading={membersLoadable.isFetching}
      windowTitle={t('organization_members_title')}
      link={LINKS.ORGANIZATION_MEMBERS}
      containerMaxWidth="md"
      navigation={[
        [
          t('organization_members_title'),
          LINKS.ORGANIZATION_MEMBERS.build({
            [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
          }),
        ],
      ]}
    >
      <Box
        mb={1}
        display="flex"
        justifyContent="space-between"
        alignItems="center"
      >
        <Typography variant="h6">{t('invitations_title')}</Typography>
        <Button
          color="primary"
          variant="contained"
          onClick={() => setInviteOpen(true)}
          data-cy="invite-generate-button"
        >
          {t('invitations_invite_button')}
        </Button>
      </Box>

      {!invitations && !invitationsLoadable.isLoading && (
        <Box m={4} display="flex" justifyContent="center">
          <Typography color="textSecondary">
            {t('invite_user_nothing_found')}
          </Typography>
        </Box>
      )}

      {invitations?.length && (
        <SimpleList
          data={invitations}
          renderItem={(i) => <InvitationItem invitation={i} />}
        />
      )}
      <InviteDialog onClose={() => setInviteOpen(false)} open={inviteOpen} />

      <Box mt={4} />
      <PaginatedHateoasList
        loadable={membersLoadable}
        title={<T keyName="organization_members_view_title" />}
        onSearchChange={setSearch}
        onPageChange={setPage}
        emptyPlaceholder={
          <Box m={4} display="flex" justifyContent="center">
            <Typography color="textSecondary">
              {t('global_nothing_found')}
            </Typography>
          </Box>
        }
        renderItem={(user) => (
          <MemberItem user={user} organizationId={organization!.id} />
        )}
      />
    </BaseOrganizationSettingsView>
  );
};
