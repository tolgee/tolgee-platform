import {
  Dialog,
  DialogTitle,
  DialogContent,
  TextField,
  DialogActions,
  Button,
  Box,
} from '@mui/material';
import { Field, Formik, Form } from 'formik';
import { useTranslate, T } from '@tolgee/react';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { FieldError } from 'tg.component/FormField';
import { NsBannerRecord } from '../context/useNsBanners';
import { useProject } from 'tg.hooks/useProject';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useTranslationsActions } from '../context/TranslationsContext';
import { confirmation } from 'tg.hooks/confirmation';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

type Props = {
  namespace: NsBannerRecord;
  onClose: () => void;
};

export const NamespaceRenameDialog: React.FC<Props> = ({
  namespace,
  onClose,
}) => {
  const { t } = useTranslate();

  const { name, id } = namespace;

  const project = useProject();

  const messaging = useMessage();
  const { refetchTranslations } = useTranslationsActions();

  const namespaceUpdate = useApiMutation({
    url: '/v2/projects/{projectId}/namespaces/{id}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}',
    options: {
      onSuccess() {
        refetchTranslations();
      },
    },
  });

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <Formik
        initialValues={{ namespace: name }}
        validationSchema={Validation.NAMESPACE_FORM}
        onSubmit={(values, helpers) => {
          if (id !== undefined) {
            confirmation({
              title: <T keyName="namespace_rename_confirmation_title" />,
              message: <T keyName="namespace_rename_confirmation_message" />,
              onConfirm: () =>
                namespaceUpdate.mutate(
                  {
                    path: { projectId: project.id, id },
                    content: { 'application/json': { name: values.namespace } },
                  },
                  {
                    onError(err) {
                      helpers.setFieldError(
                        'namespace',
                        (
                          <TranslatedError code={parseErrorResponse(err)[0]} />
                        ) as any
                      );
                    },
                    onSuccess() {
                      messaging.success(
                        <T keyName="namespace_rename_success" />
                      );
                      onClose();
                    },
                  }
                ),
            });
          }
        }}
      >
        {({ values, initialValues }) => {
          const notChanged = values.namespace === initialValues.namespace;
          return (
            <Form>
              <DialogTitle>{t('namespace_rename_title')}</DialogTitle>

              <DialogContent>
                <Field name="namespace">
                  {({ field, meta }) => (
                    <Box mt={1}>
                      <TextField
                        data-cy="namespaces-rename-text-field"
                        placeholder={t('namespace_rename_placeholder')}
                        fullWidth
                        size="small"
                        autoFocus
                        {...field}
                      />
                      <FieldError error={meta.touched && meta.error} />
                    </Box>
                  )}
                </Field>
              </DialogContent>
              <DialogActions>
                <Button data-cy="namespaces-rename-cancel" onClick={onClose}>
                  {t('namespace_rename_cancel')}
                </Button>
                <LoadingButton
                  disabled={notChanged}
                  loading={namespaceUpdate.isLoading}
                  data-cy="namespaces-rename-confirm"
                  color="primary"
                  type="submit"
                  variant="contained"
                >
                  {t('namespace_rename_confirm')}
                </LoadingButton>
              </DialogActions>
            </Form>
          );
        }}
      </Formik>
    </Dialog>
  );
};
