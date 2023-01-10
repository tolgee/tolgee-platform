import {
  TextField,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Formik, Form, Field } from 'formik';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { FieldError } from 'tg.component/FormField';

type Props = {
  namespace: string;
  onClose: () => void;
  onChange: (value: string) => void;
};

export const NamespaceNewDialog: React.FC<Props> = ({
  namespace,
  onClose,
  onChange,
}) => {
  const { t } = useTranslate();

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <Formik
        initialValues={{ namespace }}
        validationSchema={Validation.NAMESPACE_FORM}
        onSubmit={(values) => {
          onChange(values.namespace);
        }}
      >
        <Form>
          <DialogTitle>{t('namespae_select_title')}</DialogTitle>
          <DialogContent>
            <Field name="namespace">
              {({ field, meta }) => (
                <Box mt={1}>
                  <TextField
                    data-cy="namespaces-select-text-field"
                    placeholder={t('namespace_select_placeholder')}
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
            <Button data-cy="namespaces-select-cancel" onClick={onClose}>
              {t('namespace_select_cancel')}
            </Button>
            <Button
              data-cy="namespaces-select-confirm"
              color="primary"
              type="submit"
              variant="contained"
            >
              {t('namespace_select_confirm')}
            </Button>
          </DialogActions>
        </Form>
      </Formik>
    </Dialog>
  );
};
