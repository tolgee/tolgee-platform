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

import { StateType, translationStates } from 'tg.constants/translationStates';

type Props = {
  className: string;
};

export const StateSelector: React.FC<Props> = ({ className }) => {
  const t = useTranslate();

  return (
    <Field name="states">
      {({ field, meta }) => {
        return (
          <FormControl className={className} error={meta.error}>
            <InputLabel>{t('export_translations_states_label')}</InputLabel>
            <Select
              {...field}
              data-cy="export-state-selector"
              renderValue={(values: StateType[]) =>
                values
                  .map((val) => t(translationStates[val]?.translationKey))
                  .join(', ')
              }
              MenuProps={{ getContentAnchorEl: null }}
              multiple
            >
              {Object.entries(translationStates).map(([value, meta]) => (
                <MenuItem
                  key={value}
                  value={value}
                  data-cy="export-state-selector-item"
                >
                  <Checkbox checked={field.value.includes(value)} />
                  <ListItemText primary={t(meta.translationKey)} />
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
