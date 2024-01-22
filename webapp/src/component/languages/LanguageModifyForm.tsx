import { FC, FunctionComponent } from 'react';
import { Box, Button, Dialog, DialogContent } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';

import { LanguageModifyFields } from './LanguageModifyFields';

export const LanguageModifyForm: FC<{
  preferredEmojis: string[];
  values: components['schemas']['LanguageRequest'];
  onModified: (values: components['schemas']['LanguageRequest']) => void;
  onCancel: () => void;
  inDialog?: boolean;
}> = (props) => {
  const Wrapper: FunctionComponent = (wrapperProps) =>
    props.inDialog ? (
      <Dialog open={true}>
        <DialogContent>
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
          ...props.values,
          flagEmoji: props.values.flagEmoji || '🏳️',
        }}
        validationSchema={Validation.LANGUAGE(t)}
        onSubmit={(values) => {
          props.onModified(values);
        }}
      >
        {(formikProps) => (
          <Box data-cy="language-modify-form">
            <LanguageModifyFields preferredEmojis={props.preferredEmojis} />
            <Box display="flex" justifyContent="flex-end">
              <Box mr={1}>
                <Button
                  onClick={() => props.onCancel()}
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
