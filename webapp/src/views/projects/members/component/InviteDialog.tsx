import { useMemo, useState } from 'react';
import { Field, Formik } from 'formik';
import {
  Box,
  Button,
  ButtonGroup,
  Dialog,
  DialogActions,
  DialogContent,
  Typography,
  styled,
  TextField,
} from '@mui/material';
import { container } from 'tsyringe';
import { T, useTranslate } from '@tolgee/react';
import copy from 'copy-to-clipboard';

import { useProject } from 'tg.hooks/useProject';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { LINKS, PARAMS } from 'tg.constants/links';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { MessageService } from 'tg.service/MessageService';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { PermissionsSettings } from 'tg.component/PermissionsSettings/PermissionsSettings';
import {
  PermissionModel,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import {
  CreateInvitationData,
  useCreateInvitation,
} from './useCreateInvitation';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';

const messaging = container.resolve(MessageService);

const StyledContent = styled('div')`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(2)};
  min-height: 75px;
`;

type Props = {
  open: boolean;
  onClose: () => void;
};

export const InviteDialog: React.FC<Props> = ({ open, onClose }) => {
  const { t } = useTranslate();
  const project = useProject();
  const langauges = useProjectLanguages();

  const initialPermissions: PermissionModel = {
    type: 'TRANSLATE',
    scopes: [],
  };

  const { createInvitation, isLoading } = useCreateInvitation({
    projectId: project.id,
    allLangs: langauges,
  });

  const [settingsState, setSettingsState] = useState<
    PermissionSettingsState | undefined
  >(undefined);

  const yupSchema = useMemo(() => Validation.INVITE_DIALOG_PROJECT(t), [t]);

  async function handleCreateInvitation(data: CreateInvitationData) {
    try {
      const result = await createInvitation(data);
      if (!result.invitedUserEmail) {
        copy(
          LINKS.ACCEPT_INVITATION.buildWithOrigin({
            [PARAMS.INVITATION_CODE]: result.code,
          })
        );
        messaging.success(<T keyName="invite_user_invitation_copy_success" />);
      } else {
        messaging.success(<T keyName="invite_user_invitation_email_success" />);
      }
      onClose();
    } catch (e) {
      parseErrorResponse(e).forEach((e_1) => messaging.error(<T>{e_1}</T>));
    }
  }

  return (
    <Dialog {...{ open, onClose }} fullWidth>
      <Formik
        initialValues={{
          type: 'email' as 'email' | 'link',
          text: '',
        }}
        validationSchema={yupSchema}
        validateOnMount={true}
        onSubmit={(data) => {
          if (settingsState) {
            return handleCreateInvitation({
              email: data.type === 'email' ? data.text : undefined,
              name: data.type === 'link' ? data.text : undefined,
              permissions: settingsState,
            });
          }
        }}
      >
        {({ values, handleSubmit, isValid, ...formik }) => {
          return (
            <form onSubmit={handleSubmit}>
              <DialogContent>
                <Box display="flex" justifyContent="space-between" mb={2}>
                  <Typography variant="h5">
                    {t('project_members_dialog_title')}
                  </Typography>
                  <ButtonGroup>
                    <Button
                      size="small"
                      disableElevation
                      color={values.type === 'email' ? 'primary' : 'default'}
                      onClick={() => formik.setFieldValue('type', 'email')}
                      data-cy="invitation-dialog-type-email-button"
                    >
                      {t('invite_type_email')}
                    </Button>
                    <Button
                      size="small"
                      disableElevation
                      color={values.type === 'link' ? 'primary' : 'default'}
                      onClick={() => formik.setFieldValue('type', 'link')}
                      data-cy="invitation-dialog-type-link-button"
                    >
                      {t('invite_type_link')}
                    </Button>
                  </ButtonGroup>
                </Box>
                <StyledContent>
                  <Field name="text">
                    {({ field, meta }) => (
                      <TextField
                        variant="standard"
                        data-cy="invitation-dialog-input-field"
                        type={values.type === 'email' ? 'email' : 'text'}
                        label={
                          values.type === 'email'
                            ? t('project_members_dialog_email')
                            : t('project_members_dialog_name')
                        }
                        error={Boolean(meta.touched && meta.error)}
                        helperText={meta.touched && meta.error}
                        {...field}
                      />
                    )}
                  </Field>
                </StyledContent>
              </DialogContent>

              <DialogContent>
                <PermissionsSettings
                  title={t('project_members_dialog_permission_title')}
                  permissions={initialPermissions}
                  onChange={setSettingsState}
                  allLangs={langauges}
                />
              </DialogContent>
              <DialogActions>
                <Button data-cy="invitation-dialog-close-button">
                  {t('project_members_dialog_close_button')}
                </Button>
                <LoadingButton
                  variant="contained"
                  color="primary"
                  type="submit"
                  data-cy="invitation-dialog-invite-button"
                  loading={isLoading}
                >
                  {values.type === 'email'
                    ? t('project_members_dialog_invite_button')
                    : t('project_members_dialog_create_link_button')}
                </LoadingButton>
              </DialogActions>
            </form>
          );
        }}
      </Formik>
    </Dialog>
  );
};
