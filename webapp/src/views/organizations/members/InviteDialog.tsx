import { Formik, Field } from 'formik';
import {
  Dialog,
  DialogActions,
  DialogTitle,
  Button,
  DialogContent,
  TextField,
  ButtonGroup,
  Box,
  Typography,
  styled,
} from '@mui/material';
import { useTranslate, T } from '@tolgee/react';
import copy from 'copy-to-clipboard';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from '../useOrganization';
import { RoleMenu } from 'tg.component/security/RoleMenu';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { messageService } from 'tg.service/MessageService';

type RoleType = NonNullable<
  components['schemas']['OrganizationInviteUserDto']['roleType']
>;

const StyledContent = styled('div')`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(2)};
  margin-bottom: ${({ theme }) => theme.spacing(2)};
  min-height: 150px;
`;

const StyledPermissions = styled('div')`
  display: flex;
  gap: ${({ theme }) => theme.spacing(1)};
`;

type Props = {
  open: boolean;
  onClose: () => void;
};

export const InviteDialog: React.FC<Props> = ({ open, onClose }) => {
  const { t } = useTranslate();
  const organization = useOrganization();
  const invite = useApiMutation({
    url: '/v2/organizations/{id}/invite',
    method: 'put',
    invalidatePrefix: '/v2/organizations/{organizationId}/invitations',
  });

  return (
    <Dialog {...{ open, onClose }} fullWidth>
      <Formik
        initialValues={{
          role: 'MEMBER' as RoleType,
          type: 'email' as 'email' | 'link',
          text: '',
        }}
        validationSchema={Validation.INVITE_DIALOG_ORGANIZATION(t)}
        validateOnMount={true}
        onSubmit={(data) => {
          invite.mutate(
            {
              path: { id: organization!.id },
              content: {
                'application/json': {
                  roleType: data.role,
                  email: data.type === 'email' ? data.text : undefined,
                  name: data.type === 'link' ? data.text : undefined,
                },
              },
            },
            {
              onSuccess(data) {
                if (!data.invitedUserEmail) {
                  copy(
                    LINKS.ACCEPT_INVITATION.buildWithOrigin({
                      [PARAMS.INVITATION_CODE]: data.code,
                    })
                  );
                  messageService.success(
                    <T keyName="invite_user_invitation_copy_success" />
                  );
                } else {
                  messageService.success(
                    <T keyName="invite_user_invitation_email_success" />
                  );
                }
                onClose();
              },
            }
          );
        }}
      >
        {({ values, handleSubmit, isValid, ...formik }) => {
          return (
            <form onSubmit={handleSubmit}>
              <DialogTitle>
                <Box display="flex" justifyContent="space-between">
                  <span>{t('project_members_dialog_title')}</span>
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
              </DialogTitle>
              <DialogContent>
                <StyledContent>
                  <div>
                    <Typography variant="caption">
                      {t('invite_user_organization_role_label')}
                    </Typography>
                    <StyledPermissions>
                      <RoleMenu
                        role={values.role}
                        onSelect={(role) => formik.setFieldValue('role', role)}
                        buttonProps={{
                          size: 'small',
                          // @ts-ignore
                          'data-cy': 'invitation-dialog-role-button',
                        }}
                      />
                    </StyledPermissions>
                  </div>

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
              <DialogActions>
                <LoadingButton
                  variant="contained"
                  color="primary"
                  type="submit"
                  disabled={!isValid}
                  data-cy="invitation-dialog-invite-button"
                  loading={invite.isLoading}
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
