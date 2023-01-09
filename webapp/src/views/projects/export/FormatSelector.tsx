import { Field } from 'formik';
import { MenuItem, Select, FormControl, InputLabel } from '@mui/material';
import { useTranslate } from '@tolgee/react';

export const FORMATS = ['JSON', 'XLIFF'] as const;

type Props = {
  className: string;
};

export const FormatSelector: React.FC<Props> = ({ className }) => {
  const { t } = useTranslate();

  return (
    <Field name="format">
      {({ field }) => {
        return (
          <FormControl className={className} variant="standard">
            <InputLabel>{t('export_translations_format_label')}</InputLabel>
            <Select
              {...field}
              data-cy="export-format-selector"
              variant="standard"
            >
              {FORMATS.map((format) => (
                <MenuItem
                  key={format}
                  value={format}
                  data-cy="export-format-selector-item"
                >
                  {format}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        );
      }}
    </Field>
  );
};
