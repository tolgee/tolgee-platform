import { Field } from 'formik';
import {
  MenuItem,
  Select,
  Checkbox,
  ListItemText,
  FormControl,
  InputLabel,
  FormHelperText,
} from '@material-ui/core';
import { useTranslate } from '@tolgee/react';

import { StateType } from 'tg.constants/translationStates';

type Props = {
  languages: string[];
  className: string;
};

export const LanguageSelector: React.FC<Props> = ({ languages, className }) => {
  const t = useTranslate();

  return (
    <Field name="languages">
      {({ field, meta }) => {
        return (
          <FormControl className={className} error={meta.error}>
            <InputLabel>{t('export_translations_languages_label')}</InputLabel>
            <Select
              {...field}
              data-cy="export-language-selector"
              renderValue={(values: StateType[]) => values.join(', ')}
              MenuProps={{ getContentAnchorEl: null }}
              multiple
            >
              {languages.map((lang) => (
                <MenuItem
                  key={lang}
                  value={lang}
                  data-cy="export-language-selector-item"
                >
                  <Checkbox checked={field.value.includes(lang)} />
                  <ListItemText primary={lang} />
                </MenuItem>
              ))}
            </Select>
            <FormHelperText>{meta.error}</FormHelperText>
          </FormControl>
        );
      }}
    </Field>
  );
};
