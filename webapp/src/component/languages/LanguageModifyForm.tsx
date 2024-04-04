import { FC, FunctionComponent } from 'react';
import { Box, Button, Dialog, DialogContent } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';

import { LanguageModifyFields } from './LanguageModifyFields';

type LanguageRequest = components['schemas']['LanguageRequest'];

type Props = {
  preferredEmojis: string[];
  values: LanguageRequest;
  onModified: (values: LanguageRequest) => void;
  onCancel: () => void;
  inDialog?: boolean;
  existingTags: string[];
};

export const LanguageModifyForm: FC<Props> = ({
  preferredEmojis,
  values,
  onModified,
  onCancel,
  inDialog,
  existingTags,
}) => {
  const Wrapper: FunctionComponent = (wrapperProps) =>
    inDialog ? (
      <Dialog open={true}>
        <DialogContent
          sx={{
            width: '85vw',
            maxWidth: 500,
          }}
        >
          <Box mb={2}>{wrapperProps.children}</Box>
        </DialogContent>
      </Dialog>
    ) : (
      <>{wrapperProps.children}</>
    );

  const { t } = useTranslate();

  return (
    <Wrapper>
      <Formik
        initialValues={{
          ...values,
          flagEmoji: values.flagEmoji || '🏳️',
        }}
        validationSchema={Validation.LANGUAGE(t, existingTags)}
        onSubmit={(values) => {
          onModified(values);
        }}
      >
        {(formikProps) => (
          <Box data-cy="language-modify-form">
            <LanguageModifyFields preferredEmojis={preferredEmojis} />
            <Box display="flex" justifyContent="flex-end">
              <Box mr={1}>
                <Button
                  onClick={() => onCancel()}
                  data-cy="languages-modify-cancel-button"
                >
                  <T keyName="global_form_cancel" />
                </Button>
              </Box>
              <Button
                variant="contained"
                color="primary"
                type="submit"
                data-cy="languages-modify-apply-button"
                onClick={formikProps.submitForm}
              >
                <T keyName="languages_modify_ok_button" />
              </Button>
            </Box>
          </Box>
        )}
      </Formik>
    </Wrapper>
  );
};
