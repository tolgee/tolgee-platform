import { FunctionComponent, useState } from 'react';
import { Button, TextField } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import ListItemText from '@material-ui/core/ListItemText';
import Typography from '@material-ui/core/Typography';
import { T, useTranslate } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';
import { container } from 'tsyringe';

import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { MessageService } from 'tg.service/MessageService';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { BaseView } from 'tg.component/layout/BaseView';
import { PermissionSelect } from 'tg.component/security/PermissionSelect';
import { LINKS, PARAMS } from 'tg.constants/links';
import { projectPermissionTypes } from 'tg.constants/projectPermissionTypes';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { ProjectInvitationActions } from 'tg.store/project/invitations/ProjectInvitationActions';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import LoadingButton from 'tg.component/common/form/LoadingButton';

const messaging = container.resolve(MessageService);

type FormType = {
  type: string;
};

const actions = container.resolve(ProjectInvitationActions);

export const ProjectInviteView: FunctionComponent = () => {
  const match = useRouteMatch();
  const projectId = match.params[PARAMS.PROJECT_ID];

  const [code, setCode] = useState('');

  const project = useProject();

  const t = useTranslate();

  const invitations = useApiQuery({
    url: '/api/invitation/list/{projectId}',
    method: 'get',
    path: { projectId: Number(projectId) },
  });

  const invite = useApiMutation({
    url: '/v2/projects/{projectId}/invite',
    method: 'put',
  });
  const deleteInvitation = useApiMutation({
    url: '/api/invitation/{invitationId}',
    method: 'delete',
    fetchOptions: { disableNotFoundHandling: true },
    options: {
      onError(e) {
        messaging.error(parseErrorResponse(e));
        invitations.refetch();
      },
    },
  });

  const handleSubmit = (values: FormType) => {
    invite.mutate(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            type: values.type as any,
          },
        },
      },
      {
        onSuccess: (code) => {
          actions.setCode.dispatch(code);
          invitations.refetch();
          setCode(code);
        },
      }
    );
  };

  const handleCancel = (invitationId: number) => {
    deleteInvitation.mutate(
      { path: { invitationId } },
      {
        onSuccess: () => {
          actions.setCode.dispatch('');
          invitations.refetch();
        },
      }
    );
  };

  return (
    <BaseView
      loading={invitations.isFetching || deleteInvitation.isLoading}
      hideChildrenOnLoading={false}
      navigation={[
        [
          project.name,
          LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
        [
          t('invite_user_title'),
          LINKS.PROJECT_INVITATION.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
      lg={7}
      md={9}
      containerMaxWidth="lg"
    >
      {() => (
        <>
          <StandardForm
            saveActionLoadable={invite}
            submitButtons={
              <LoadingButton
                variant="contained"
                color="primary"
                type="submit"
                size="large"
                loading={invite.isLoading}
                data-cy="invite-generate-button"
              >
                <T>invite_user_generate_invitation_link</T>
              </LoadingButton>
            }
            onSubmit={handleSubmit}
            initialValues={{ type: 'MANAGE' }}
          >
            <PermissionSelect
              label={<T>invite_user_permission_label</T>}
              name="type"
              fullWidth
            />
          </StandardForm>
          {code && (
            <Box mt={2}>
              <TextField
                fullWidth
                multiline
                InputProps={{
                  readOnly: true,
                }}
                value={LINKS.ACCEPT_INVITATION.buildWithOrigin({
                  [PARAMS.INVITATION_CODE]: code,
                })}
                label={<T>invite_user_invitation_code</T>}
                data-cy="invite-generate-input-code"
              />
            </Box>
          )}
          {invitations.data?.length ? (
            <Box mt={4}>
              <Typography variant="h6">
                <T>invite_user_active_invitation_codes</T>
              </Typography>
              <List>
                {invitations.data.map((i) => (
                  <ListItem key={i.id}>
                    <ListItemText>
                      {i!.code!.substr(0, 10)}...
                      {i!.code!.substr(i!.code!.length - 10, 10)}
                      &nbsp;[
                      <i>
                        <T>invite_user_permission_label</T>:
                        <T>{`permission_type_${
                          projectPermissionTypes[i!.type!]
                        }`}</T>
                      </i>
                      ]
                    </ListItemText>
                    <ListItemSecondaryAction>
                      <Button
                        color="secondary"
                        onClick={() => handleCancel(i!.id!)}
                      >
                        <T>invite_user_invitation_cancel_button</T>
                      </Button>
                    </ListItemSecondaryAction>
                  </ListItem>
                ))}
              </List>
            </Box>
          ) : (
            <EmptyListMessage
              loading={invitations.isFetching || deleteInvitation.isLoading}
            >
              <T>invite_user_nothing_found</T>
            </EmptyListMessage>
          )}
        </>
      )}
    </BaseView>
  );
};
