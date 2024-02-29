import { useState } from 'react';
import { Field, useFormikContext } from 'formik';
import { ExpandLess, ExpandMore } from '@mui/icons-material';
import {
  Box,
  Checkbox,
  FormControlLabel,
  IconButton,
  TextField,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { FieldError, FieldLabel } from 'tg.component/FormField';

import { LabelHint } from '../LabelHint';

type Props = {
  pluralParameterName: string;
  isPluralName: string;
};

export const PluralFormCheckbox = ({
  pluralParameterName,
  isPluralName,
}: Props) => {
  const { values } = useFormikContext<any>();
  const [expanded, setExpanded] = useState(
    values[isPluralName] && values[pluralParameterName] !== 'value'
  );
  const isPlural = values[isPluralName];
  const { t } = useTranslate();

  return (
    <Box display="grid">
      <Field name={isPluralName}>
        {({ field }) => (
          <Box justifyContent="start" display="flex" alignItems="center">
            <FormControlLabel
              data-cy="key-plural-checkbox"
              control={<Checkbox checked={Boolean(field.value)} {...field} />}
              label={t('translation_single_label_is_plural')}
            />
            <IconButton
              size="small"
              onClick={() => setExpanded((val) => !val)}
              disabled={!isPlural}
              data-cy="key-plural-checkbox-expand"
            >
              {expanded ? (
                <ExpandLess fontSize="small" />
              ) : (
                <ExpandMore fontSize="small" />
              )}
            </IconButton>
          </Box>
        )}
      </Field>

      {expanded && (
        <Field name={pluralParameterName}>
          {({ field, meta }) => (
            <Box display="grid">
              <FieldLabel>
                <LabelHint title={t('translation_single_label_plural_hint')}>
                  <T keyName="translation_single_label_plural_variable" />
                </LabelHint>
              </FieldLabel>
              <TextField
                {...field}
                size="small"
                disabled={!isPlural}
                sx={{ maxWidth: 300 }}
                data-cy="key-plural-variable-name"
              />
              <FieldError error={meta.touched && meta.error} />
            </Box>
          )}
        </Field>
      )}
    </Box>
  );
};
