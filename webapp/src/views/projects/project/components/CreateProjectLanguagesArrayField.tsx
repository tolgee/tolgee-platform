import { FC } from 'react';
import { Box, Grid } from '@mui/material';
import FormHelperText from '@mui/material/FormHelperText';
import { Field, useFormikContext } from 'formik';

import { CreateLanguagesField } from 'tg.component/languages/CreateLanguagesField';

import { CreateProjectValueType } from '../ProjectCreateView';

export const CreateProjectLanguagesArrayField: FC = () => {
  const formikContext = useFormikContext<CreateProjectValueType>();

  const languagesMeta = formikContext.getFieldMeta('languages');

  return (
    <>
      <Box display="grid">
        <Field name="languages">
          {({ field }) => {
            return (
              <CreateLanguagesField
                autoFocus={false}
                onChange={(value) =>
                  formikContext.setFieldValue(field.name, value)
                }
                value={field.value || []}
                existingTags={[]}
              />
            );
          }}
        </Field>
        {languagesMeta.touched && typeof languagesMeta.error === 'string' && (
          <Grid item xs={12}>
            <FormHelperText error={!!languagesMeta.error} color="textSecondary">
              {languagesMeta.error}
            </FormHelperText>
          </Grid>
        )}
      </Box>
    </>
  );
};
