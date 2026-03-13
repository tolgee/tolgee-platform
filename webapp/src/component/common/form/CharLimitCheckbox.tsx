import {
  Box,
  Checkbox,
  FormControlLabel,
  TextField,
  Tooltip,
} from '@mui/material';
import { useFormikContext } from 'formik';
import { useTranslate } from '@tolgee/react';
import { HelpCircle } from '@untitled-ui/icons-react';

import { FieldError, FieldLabel } from 'tg.component/FormField';

type Props = {
  fieldName: string;
};

export const CharLimitCheckbox = ({ fieldName }: Props) => {
  const { t } = useTranslate();
  const { values, errors, setFieldValue } = useFormikContext<any>();

  const hasCharLimit = values[fieldName] !== undefined;

  return (
    <Box display="grid">
      <Box justifyContent="start" display="flex" alignItems="center">
        <FormControlLabel
          data-cy="key-char-limit-checkbox"
          control={
            <Checkbox
              checked={hasCharLimit}
              onChange={(e) => {
                setFieldValue(fieldName, e.target.checked ? 0 : undefined);
              }}
            />
          }
          label={
            <Box display="inline-flex" alignItems="center" gap="4px">
              {t('translation_single_label_max_char_limit')}
              <Tooltip
                title={t('translation_single_max_char_limit_hint')}
                disableInteractive
              >
                <Box component="span" display="inline-flex">
                  <HelpCircle style={{ width: 15, height: 15 }} />
                </Box>
              </Tooltip>
            </Box>
          }
          sx={{ mr: 0.5 }}
        />
      </Box>
      {hasCharLimit && (
        <Box display="grid">
          <FieldLabel>
            {t('translation_single_label_char_limit_maximum')}
          </FieldLabel>
          <TextField
            data-cy="key-char-limit-input"
            type="number"
            size="small"
            value={values[fieldName] || ''}
            onChange={(e) => {
              const val = e.target.value;
              setFieldValue(
                fieldName,
                val === '' ? 0 : Math.max(1, parseInt(val, 10))
              );
            }}
            inputProps={{ min: 1 }}
            sx={{ maxWidth: 300 }}
          />
          <FieldError error={errors[fieldName]} />
        </Box>
      )}
    </Box>
  );
};
