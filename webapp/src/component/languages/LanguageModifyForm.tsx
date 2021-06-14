import { FC } from 'react';
import { T } from '@tolgee/react';
import { Box, Button } from '@material-ui/core';
import { components } from '../../service/apiSchema.generated';
import { Form, Formik } from 'formik';
import { Validation } from '../../constants/GlobalValidationSchema';
import { LanguageModifyFields } from './LanguageModifyFields';

export const LanguageModifyForm: FC<{
  preferredEmojis: string[];
  values: components['schemas']['LanguageDto'];
  onModified: (values: components['schemas']['LanguageDto']) => void;
  onCancel: () => void;
}> = ({ preferredEmojis, values, onModified, onCancel }) => {
  return (
    <Formik
      initialValues={values}
      validationSchema={Validation.LANGUAGE}
      onSubmit={(values) => {
        onModified(values);
      }}
    >
      <Form>
        <LanguageModifyFields preferredEmojis={preferredEmojis} />
        <Box display="flex" justifyContent="flex-end">
          <Box mr={1}>
            <Button onClick={() => onCancel()}>
              <T>global_form_cancel</T>
            </Button>
          </Box>
          <Button variant="contained" color="primary" type="submit">
            <T>languages_modify_ok_button</T>
          </Button>
        </Box>
      </Form>
    </Formik>
  );
};
