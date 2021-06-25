import { FunctionComponent, useState } from 'react';
import { Button, ListItemText, TextField, Typography } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import { T } from '@tolgee/react';

import { StandardForm } from 'tg.component/common/form/StandardForm';
import { SimpleList } from 'tg.component/common/list/SimpleList';
import { SimpleListItem } from 'tg.component/common/list/SimpleListItem';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { OrganizationRoleType } from 'tg.service/response.types';

import { BaseOrganizationSettingsView } from './BaseOrganizationSettingsView';
import { OrganizationRoleSelect } from './components/OrganizationRoleSelect';
import { useOrganization } from './useOrganization';

export const OrganizationInvitationsView: FunctionComponent = () => {
  const organization = useOrganization();

  const [code, setCode] = useState('');

  const invitationsLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/invitations',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });
  const inviteLoadable = useApiMutation({
    url: '/v2/organizations/{id}/invite',
    method: 'put',
  });
  const cancelInviteLoadable = useApiMutation({
    url: '/api/invitation/{invitationId}',
    method: 'delete',
  });

  const onSubmit = (values) => {
    inviteLoadable.mutate(
      {
        path: { id: organization!.id },
        content: { 'application/json': { roleType: values.type } },
      },
      {
        onSuccess(data) {
          setCode(data.code);
          invitationsLoadable.refetch();
        },
      }
    );
  };

  const onCancel = (invitationId: number) => {
    cancelInviteLoadable.mutate(
      { path: { invitationId } },
      {
        onSuccess() {
          setCode('');
          invitationsLoadable.refetch();
        },
      }
    );
  };

  return (
    <BaseOrganizationSettingsView title={<T>organization_invitations_title</T>}>
      <StandardForm
        loading={inviteLoadable.isLoading}
        submitButtons={
          <Button
            data-cy="organization-invitation-generate-button"
            variant="contained"
            color="primary"
            type="submit"
            size="large"
          >
            <T>invite_user_generate_invitation_link</T>
          </Button>
        }
        onSubmit={onSubmit}
        initialValues={{ type: OrganizationRoleType.MEMBER }}
      >
        <OrganizationRoleSelect
          data-cy="organization-invitation-role-select"
          label={<T>invite_user_organization_role_label</T>}
          name="type"
          fullWidth
        />
      </StandardForm>

      {code && (
        <Box mt={2}>
          <TextField
            data-cy="organization-invitations-generated-field"
            fullWidth
            multiline
            InputProps={{
              readOnly: true,
            }}
            value={LINKS.ACCEPT_INVITATION.buildWithOrigin({
              [PARAMS.INVITATION_CODE]: code,
            })}
            label={<T>invite_user_invitation_code</T>}
          />
        </Box>
      )}

      <Box mt={4}>
        <Typography variant="h6">
          <T>invite_user_active_invitation_codes</T>
        </Typography>
        <Box mt={2}>
          {invitationsLoadable.data?._embedded?.organizationInvitations && (
            <SimpleList
              data={invitationsLoadable.data._embedded.organizationInvitations}
              renderItem={(i) => (
                <SimpleListItem key={i.id}>
                  <ListItemText>
                    {i.code.substr(0, 10)}...
                    {i.code.substr(i.code.length - 10, 10)}
                    &nbsp;[
                    <i>
                      <T>invite_user_organization_role_label</T>:
                      <T>{`organization_role_type_${
                        OrganizationRoleType[i.type]
                      }`}</T>
                    </i>
                    ]
                  </ListItemText>
                  <ListItemSecondaryAction>
                    <Button
                      data-cy="organization-invitation-cancel-button"
                      color="secondary"
                      onClick={() => onCancel(i.id)}
                    >
                      <T>invite_user_invitation_cancel_button</T>
                    </Button>
                  </ListItemSecondaryAction>
                </SimpleListItem>
              )}
            />
          )}
        </Box>
      </Box>
    </BaseOrganizationSettingsView>
  );
};
