import { Box } from '@mui/material';
import { T } from '@tolgee/react';
import { Field, FieldProps } from 'formik';
import { FieldError } from 'tg.component/FormField';

import { EditorJson } from 'tg.component/editor/EditorJson';
import { EditorWrapper } from 'tg.component/editor/EditorWrapper';

export const KeyCustomValues = () => {
  return (
    <Box mb={2} display="grid" gap={2}>
      <Box>
        <T keyName="translations_key_edit_custom_properties_description" />
      </Box>
      <Field name="custom">
        {({ field, form, meta }: FieldProps<any>) => (
          <Box display="grid" gap={0.5}>
            <EditorWrapper>
              <EditorJson
                value={field.value}
                onChange={(value) => form.setFieldValue(field.name, value)}
                minHeight="150px"
              />
            </EditorWrapper>
            <FieldError error={meta.error} />
          </Box>
        )}
      </Field>
    </Box>
  );
};
