import * as React from 'react';
import { FC } from 'react';
import { FieldArray } from '../../../../common/form/fields/FieldArray';
import { Box, Button, Grid } from '@material-ui/core';
import { T } from '@tolgee/react';
import FormHelperText from '@material-ui/core/FormHelperText';
import { useField, useFormikContext } from 'formik';
import { CreateLanguageField } from '../../../../languages/CreateLanguageField';
import { CreateProjectValueType } from '../ProjectCreateView';

export const CreateProjectLanguagesArrayField: FC = () => {
  const formikContext = useFormikContext<CreateProjectValueType>();
  const hasUnfinishedLanguage =
    formikContext.values.languages.findIndex((l) => l === null || !l?.name) >
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
              <Box display="inline-flex" alignItems="center">
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
          {(_, index, removeItem) => (
            <CreateLanguageFormikField
              key={index}
              name="languages"
              index={index}
              onRemove={removeItem}
            />
          )}
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

const CreateLanguageFormikField: FC<{
  name: string;
  index: number;
  onRemove: () => void;
}> = (props) => {
  const [inputProps, _, helpers] = useField(`${props.name}.${props.index}`);
  const isPrepared = !!inputProps.value;

  return (
    <Grid item {...{ xs: !isPrepared && 12 }}>
      <CreateLanguageField
        autoFocus={true}
        modifyInDialog={true}
        onChange={helpers.setValue}
        value={inputProps.value}
        showSubmitButton={false}
        onPreparedClear={props.onRemove}
        onAutocompleteClear={props.onRemove}
        preparedLanguageWrapperProps={{ display: 'inline-flex' }}
      />
    </Grid>
  );
};
