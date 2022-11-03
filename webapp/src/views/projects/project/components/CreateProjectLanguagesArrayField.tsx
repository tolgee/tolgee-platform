import { FC } from 'react';
import { Box, Button, Grid } from '@mui/material';
import FormHelperText from '@mui/material/FormHelperText';
import { T } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { FieldArray } from 'tg.component/common/form/fields/FieldArray';
import { CreateLanguageField } from 'tg.component/languages/CreateLanguageField';

import { CreateProjectValueType } from '../ProjectCreateView';

export const CreateProjectLanguagesArrayField: FC = () => {
  const formikContext = useFormikContext<CreateProjectValueType>();
  const hasUnfinishedLanguage =
    formikContext.values.languages!.findIndex((l) => l === null || !l?.name) >
    -1;
  const languagesMeta = formikContext.getFieldMeta('languages');

  return (
    <>
      <Grid container spacing={2}>
        <FieldArray
          defaultItemValue={null}
          showArrayErrors={!hasUnfinishedLanguage}
          name="languages"
          addButton={(addItem) =>
            !hasUnfinishedLanguage && (
              <Box display="inline-flex" alignItems="center" mt={2} ml={2}>
                <Button
                  data-cy="create-project-language-add-button"
                  onClick={addItem}
                  color="primary"
                >
                  <T>language_create_add</T>
                </Button>
              </Box>
            )
          }
        >
          {(_, index, removeItem) => {
            const fieldName = `languages.${index}`;
            const helpers = formikContext.getFieldHelpers(fieldName);
            const inputProps = formikContext.getFieldProps(fieldName);
            const isPrepared = !!inputProps.value;
            return (
              <Grid item {...{ xs: !isPrepared && 12 }} spacing={2}>
                <CreateLanguageField
                  key={index}
                  autoFocus={true}
                  modifyInDialog={true}
                  onChange={helpers.setValue}
                  value={inputProps.value}
                  showSubmitButton={false}
                  onPreparedClear={removeItem}
                  onAutocompleteClear={removeItem}
                  preparedLanguageWrapperProps={{ display: 'inline-flex' }}
                />
              </Grid>
            );
          }}
        </FieldArray>
        {languagesMeta.touched && typeof languagesMeta.error === 'string' && (
          <Grid item xs={12}>
            <FormHelperText error={!!languagesMeta.error} color="textSecondary">
              {languagesMeta.error}
            </FormHelperText>
          </Grid>
        )}
      </Grid>
    </>
  );
};
