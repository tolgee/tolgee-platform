import { useEffect, useState } from 'react';
import { Field, useFormikContext } from 'formik';
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
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

function isParameterDefault(value: string | undefined) {
  return value === undefined || value === 'value';
}

type Props = {
  pluralParameterName: string;
  isPluralName: string;
};

export const PluralFormCheckbox = ({
  pluralParameterName,
  isPluralName,
}: Props) => {
  const { values } = useFormikContext<any>();
  const [_expanded, setExpanded] = useState(
    !isParameterDefault(values[pluralParameterName])
  );

  useEffect(() => {
    if (
      values[isPluralName] &&
      !isParameterDefault(values[pluralParameterName])
    ) {
      setExpanded(true);
    }
  }, [values[pluralParameterName]]);

  const isPlural = values[isPluralName];
  const expanded = _expanded && isPlural;
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
              sx={{ mr: 0.5 }}
            />
            <IconButton
              size="small"
              onClick={() => setExpanded((val) => !val)}
              disabled={!isPlural}
              data-cy="key-plural-checkbox-expand"
            >
              {expanded ? <ChevronUp /> : <ChevronDown />}
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
