import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  styled,
} from '@mui/material';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { Formik } from 'formik';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { Validation } from 'tg.constants/GlobalValidationSchema';

type WebhookConfigModel = components['schemas']['WebhookConfigModel'];

const StyledDialogContent = styled(DialogContent)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(3)};
  margin-top: 8px;
  grid-template-columns: 1fr 1fr;
  width: 85vw;
  max-width: 500px;
`;

type Props = {
  onClose: () => void;
  data?: WebhookConfigModel;
};

export const WebhookEditDialog = ({ onClose, data }: Props) => {
  const { t } = useTranslate();
  const project = useProject();
  const messaging = useMessage();

  const createWebhook = useApiMutation({
    url: '/v2/projects/{projectId}/webhook-configs',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/webhook-configs',
  });

  const updateWebhook = useApiMutation({
    url: '/v2/projects/{projectId}/webhook-configs/{id}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/webhook-configs',
  });

  const deleteItem = useApiMutation({
    url: '/v2/projects/{projectId}/webhook-configs/{id}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/webhook-configs',
  });

  function handleDelete() {
    confirmation({
      title: <T keyName="webhook_item_delete_dialog_title" />,
      onConfirm() {
        deleteItem.mutate(
          {
            path: { projectId: project.id, id: data!.id },
          },
          {
            onSuccess() {
              onClose();
              messaging.success(<T keyName="webhook_delete_success" />);
            },
          }
        );
      },
    });
  }

  const isSubmitting = createWebhook.isLoading || updateWebhook.isLoading;

  return (
    <Dialog open onClose={onClose} maxWidth="sm">
      <Formik
        initialValues={{
          url: data?.url ?? '',
        }}
        validationSchema={Validation.WEBHOOK_FORM}
        validateOnBlur={false}
        enableReinitialize={false}
        onSubmit={(values, actions) => {
          if (data) {
            updateWebhook.mutate(
              {
                path: { projectId: project.id, id: data!.id },
                content: {
                  'application/json': values,
                },
              },
              {
                onSuccess() {
                  onClose();
                  messaging.success(<T keyName="webhook_edit_success" />);
                },
                onSettled() {
                  actions.setSubmitting(false);
                },
              }
            );
          } else {
            createWebhook.mutate(
              {
                path: { projectId: project.id },
                content: {
                  'application/json': values,
                },
              },
              {
                onSuccess() {
                  onClose();
                  messaging.success(<T keyName="webhook_create_success" />);
                },
                onSettled() {
                  actions.setSubmitting(false);
                },
              }
            );
          }
        }}
      >
        {({ handleSubmit }) => (
          <>
            <DialogTitle>
              {data ? t('webhook_update_title') : t('webhook_create_title')}
            </DialogTitle>
            <StyledDialogContent>
              <Box sx={{ gridColumn: '1 / span 2', display: 'grid' }}>
                <TextField
                  name="url"
                  label={t('webhook_form_url_label')}
                  variant="standard"
                  data-cy="webhook-form-url"
                />
              </Box>
            </StyledDialogContent>
            <DialogActions sx={{ justifyContent: 'space-between' }}>
              <div>
                {data && (
                  <Button
                    onClick={handleDelete}
                    variant="outlined"
                    data-cy="webhook-form-delete"
                  >
                    {t('webhook_form_delete')}
                  </Button>
                )}
              </div>
              <Box display="flex" gap={1}>
                <Button onClick={onClose} data-cy="webhook-form-cancel">
                  {t('webhook_form_cancel')}
                </Button>
                <LoadingButton
                  variant="contained"
                  color="primary"
                  onClick={() => handleSubmit()}
                  loading={isSubmitting}
                  data-cy="webhook-form-save"
                >
                  {t('webhook_form_save')}
                </LoadingButton>
              </Box>
            </DialogActions>
          </>
        )}
      </Formik>
    </Dialog>
  );
};
