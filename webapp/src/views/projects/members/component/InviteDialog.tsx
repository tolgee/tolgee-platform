import { useMemo, useState } from 'react';
import { Field, Formik } from 'formik';
import {
  Box,
  Button,
  ButtonGroup,
  Dialog,
  DialogActions,
  DialogContent,
  styled,
  TextField,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import copy from 'copy-to-clipboard';

import { useProject } from 'tg.hooks/useProject';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { LINKS, PARAMS } from 'tg.constants/links';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { PermissionsSettings } from 'tg.component/PermissionsSettings/PermissionsSettings';
import {
  PermissionModel,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { messageService } from 'tg.service/MessageService';

import {
  CreateInvitationData,
  useCreateInvitation,
} from './useCreateInvitation';
import { useConfig, useEnabledFeatures } from 'tg.globalContext/helpers';
import { AgencySelect } from './AgencySelect';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { ShoppingCart01 } from '@untitled-ui/icons-react';

const StyledContent = styled('div')`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(2)};
  min-height: 75px;
  margin-bottom: 20px;
`;

const StyledButton = styled('span')`
  background: ${({ theme }) => theme.palette.tokens.background['paper-1']};
  border-radius: 3px;
  padding: 2px 6px;
`;

type Props = {
  open: boolean;
  onClose: () => void;
};

export const InviteDialog: React.FC<Props> = ({ open, onClose }) => {
  const { t } = useTranslate();
  const project = useProject();
  const langauges = useProjectLanguages();
  const config = useConfig();
  const { isEnabled } = useEnabledFeatures();

  const agencyEnabled =
    config.billing.enabled && isEnabled('ORDER_TRANSLATION');

  const preferredAgencyLoadable = useBillingApiQuery({
    url: '/v2/projects/{projectId}/billing/order-translation/preferred-agency',
    method: 'get',
    options: {
      enabled: agencyEnabled,
    },
    path: {
      projectId: project.id,
    },
  });

  const preferredAgency = preferredAgencyLoadable.isLoading
    ? undefined
    : preferredAgencyLoadable.data?.preferredAgencyId ?? false;

  const agencyTaskExists = typeof preferredAgency === 'number';

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
    const result = await createInvitation(data);
    if (!result.invitedUserEmail && result.code) {
      copy(
        LINKS.ACCEPT_INVITATION.buildWithOrigin({
          [PARAMS.INVITATION_CODE]: result.code,
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
  }

  return (
    <Dialog {...{ open, onClose }} fullWidth>
      <Formik
        initialValues={{
          type: 'email' as 'email' | 'link' | 'agency',
          text: '',
          agency: preferredAgency?.toString() ?? '',
        }}
        enableReinitialize
        validationSchema={yupSchema}
        validateOnMount={true}
        onSubmit={(data) => {
          if (settingsState) {
            return handleCreateInvitation({
              email: data.type === 'email' ? data.text : undefined,
              name: data.type === 'link' ? data.text : undefined,
              agency: data.type === 'agency' ? data.agency : undefined,
              permissions: settingsState,
            });
          }
        }}
      >
        {({ values, handleSubmit, isValid, ...formik }) => {
          const disabled = values.type === 'agency' && !agencyTaskExists;
          return (
            <form onSubmit={handleSubmit}>
              <DialogContent sx={{ height: 'min(80vh, 700px)' }}>
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
                    {agencyEnabled && (
                      <Button
                        size="small"
                        disableElevation
                        color={values.type === 'agency' ? 'primary' : 'default'}
                        onClick={() => formik.setFieldValue('type', 'agency')}
                        data-cy="invitation-dialog-type-agency-button"
                      >
                        {t('invite_type_agency')}
                      </Button>
                    )}
                  </ButtonGroup>
                </Box>
                <StyledContent>
                  {values.type === 'agency' ? (
                    preferredAgencyLoadable.data &&
                    (!agencyTaskExists ? (
                      <div>
                        <T
                          keyName="project_members_dialog_agency_task_explanation"
                          params={{
                            taskSection: (
                              <StyledButton>
                                {t('project_menu_tasks')}
                              </StyledButton>
                            ),
                            orderButton: (
                              <StyledButton>
                                <ShoppingCart01 height={14} width={14} />{' '}
                                {t('tasks_order_translation')}
                              </StyledButton>
                            ),
                          }}
                        />
                      </div>
                    ) : (
                      <Field name="agency">
                        {({ field, meta }) => {
                          return (
                            <AgencySelect
                              value={field.value}
                              onChange={(value) =>
                                formik.setFieldValue(field.name, value)
                              }
                              error={Boolean(meta.touched && meta.error)}
                              helperText={meta.touched && meta.error}
                            />
                          );
                        }}
                      </Field>
                    ))
                  ) : (
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
                  )}
                </StyledContent>

                <PermissionsSettings
                  title={t('project_members_dialog_permission_title')}
                  permissions={initialPermissions}
                  onChange={setSettingsState}
                  allLangs={langauges}
                  hideNone
                  disabled={disabled}
                />
              </DialogContent>
              <DialogActions>
                <Button
                  onClick={onClose}
                  data-cy="invitation-dialog-close-button"
                >
                  {t('project_members_dialog_close_button')}
                </Button>
                <LoadingButton
                  variant="contained"
                  color="primary"
                  type="submit"
                  data-cy="invitation-dialog-invite-button"
                  loading={isLoading}
                  disabled={disabled}
                >
                  {values.type !== 'link'
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
