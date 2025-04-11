import { Box, Button } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Form, Formik } from 'formik';

import { TextField } from 'tg.component/common/form/fields/TextField';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import React from 'react';
import { Link } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { Validation } from 'tg.constants/GlobalValidationSchema';

export type TAFormData = {
  name: string;
  description: string;
  services: string[];
  url: string;
  email: string;
  emailBcc: string[];
};

type Props = {
  initialData: Partial<TAFormData>;
  onSubmit: (value: TAFormData) => void;
  loading: boolean | undefined;
  avatarEdit?: React.ReactNode;
};

export function TAEditForm({
  initialData,
  onSubmit,
  loading,
  avatarEdit,
}: Props) {
  const { t } = useTranslate();
  return (
    <Formik
      initialValues={{
        name: initialData.name ?? '',
        description: initialData.description ?? '',
        services: initialData.services?.join('; ') ?? '',
        url: initialData.url ?? '',
        email: initialData.email ?? '',
        emailBcc: initialData.emailBcc?.join('; ') ?? '',
      }}
      enableReinitialize
      validationSchema={Validation.TRANSLATION_AGENCY_FORM()}
      onSubmit={(values) => {
        onSubmit({
          ...values,
          services: values.services
            .split(';')
            .map((s) => s.trim())
            .filter(Boolean),
          emailBcc: values.emailBcc
            .split(';')
            .map((v) => v.trim())
            .filter(Boolean),
        });
      }}
    >
      {() => (
        <Form>
          <Box pt={2}>
            <Box display="grid" gridAutoFlow="column">
              <Box>
                <TextField
                  name="name"
                  size="small"
                  label="Name"
                  fullWidth
                  data-cy="administration-ee-translation-agencies-field-name"
                />
                <TextField
                  name="email"
                  size="small"
                  label="Contact email"
                  fullWidth
                  data-cy="administration-ee-translation-agencies-field-email"
                />
              </Box>
              {avatarEdit && (
                <Box display="flex" justifyContent="center">
                  {avatarEdit}
                </Box>
              )}
            </Box>
            <TextField
              name="emailBcc"
              size="small"
              label='Hidden email copy (separated by ";")'
              fullWidth
              data-cy="administration-ee-translation-agencies-field-email-bcc"
            />
            <TextField
              name="url"
              size="small"
              label="Url"
              fullWidth
              data-cy="administration-ee-translation-agencies-field-url"
            />
            <TextField
              name="description"
              multiline
              minRows={2}
              size="small"
              label="Description (Markdown)"
              fullWidth
              sx={{ mb: 2 }}
              data-cy="administration-ee-translation-agencies-field-description"
            />
            <TextField
              name="services"
              multiline
              minRows={2}
              size="small"
              label='Services (separated by ";")'
              fullWidth
              sx={{ mb: 2 }}
              data-cy="administration-ee-translation-agencies-field-services"
            />
          </Box>
          <Box display="flex" justifyContent="end" mt={4} gap={1}>
            <Button
              component={Link}
              to={LINKS.ADMINISTRATION_EE_TA.build()}
              data-cy="administration-ee-plan-cancel-button"
            >
              {t('global_cancel_button')}
            </Button>
            <LoadingButton
              loading={loading}
              variant="contained"
              color="primary"
              type="submit"
              data-cy="form-submit-button"
            >
              {t('global_form_save')}
            </LoadingButton>
          </Box>
        </Form>
      )}
    </Formik>
  );
}
