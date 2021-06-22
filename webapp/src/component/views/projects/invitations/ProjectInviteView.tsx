import { FunctionComponent, useState } from 'react';
import { useRouteMatch } from 'react-router-dom';
import { LINKS, PARAMS } from '../../../../constants/links';
import { Button, TextField } from '@material-ui/core';
import { BaseView } from '../../../layout/BaseView';
import { container } from 'tsyringe';
import { projectPermissionTypes } from '../../../../constants/projectPermissionTypes';
import { StandardForm } from '../../../common/form/StandardForm';
import Box from '@material-ui/core/Box';
import Typography from '@material-ui/core/Typography';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import { EmptyListMessage } from '../../../common/EmptyListMessage';
import { T, useTranslate } from '@tolgee/react';
import { PermissionSelect } from '../../../security/PermissionSelect';
import { useProject } from '../../../../hooks/useProject';
import { Navigation } from '../../../navigation/Navigation';
import { ProjectInvitationActions } from '../../../../store/project/invitations/ProjectInvitationActions';
import {
  useApiMutation,
  useApiQuery,
} from '../../../../service/http/useQueryApi';

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

  const loading =
    invitations.isFetching || invite.isLoading || deleteInvitation.isLoading;

  return (
    <BaseView
      loading={loading}
      hideChildrenOnLoading={false}
      navigation={
        <Navigation
          path={[
            [
              project.name,
              LINKS.PROJECT_TRANSLATIONS.build({
                [PARAMS.PROJECT_ID]: project.id,
              }),
            ],
            [
              t('invite_user_title'),
              LINKS.PROJECT_TRANSLATIONS.build({
                [PARAMS.PROJECT_ID]: project.id,
              }),
            ],
          ]}
        />
      }
      xs={12}
      md={8}
      lg={6}
    >
      {() => (
        <>
          <StandardForm
            submitButtons={
              <Button
                variant="contained"
                color="primary"
                type="submit"
                size="large"
                disabled={invite.isLoading}
              >
                <T>invite_user_generate_invitation_link</T>
              </Button>
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
              />
            </Box>
          )}
          {invitations.data &&
            (invitations.data.length ? (
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
                          disabled={
                            deleteInvitation.isLoading || invitations.isFetching
                          }
                        >
                          <T>invite_user_invitation_cancel_button</T>
                        </Button>
                      </ListItemSecondaryAction>
                    </ListItem>
                  ))}
                </List>
              </Box>
            ) : (
              <EmptyListMessage />
            ))}
        </>
      )}
    </BaseView>
  );
};
