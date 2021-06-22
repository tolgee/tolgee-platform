import { FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import { LINKS, PARAMS } from '../../../constants/links';
import { StandardForm } from '../../common/form/StandardForm';
import { BaseOrganizationSettingsView } from './BaseOrganizationSettingsView';
import { useOrganization } from './useOrganization';
import { Button, ListItemText, TextField, Typography } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import { OrganizationRoleSelect } from './components/OrganizationRoleSelect';
import { OrganizationRoleType } from '../../../service/response.types';
import { SimpleListItem } from '../../common/list/SimpleListItem';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import { SimpleList } from '../../common/list/SimpleList';
import { useState } from 'react';
import { useApiMutation, useApiQuery } from '../../../service/http/useQueryApi';

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
