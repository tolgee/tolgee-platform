import { Field } from 'formik';
import {
  Checkbox,
  FormControl,
  FormHelperText,
  InputLabel,
  ListItemText,
  MenuItem,
  Select,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { StateType, translationStates } from 'tg.constants/translationStates';
import { exportableStates } from './ExportForm';

type Props = {
  className: string;
};

export const StateSelector: React.FC<Props> = ({ className }) => {
  const { t } = useTranslate();

  return (
    <Field name="states">
      {({ field, meta }) => {
        return (
          <FormControl
            className={className}
            error={meta.error}
            variant="standard"
          >
            <InputLabel>{t('export_translations_states_label')}</InputLabel>
            <Select
              {...field}
              variant="standard"
              data-cy="export-state-selector"
              renderValue={(values: StateType[]) =>
                values
                  .map((val) => t(translationStates[val]?.translationKey))
                  .join(', ')
              }
              multiple
            >
              {exportableStates.map((state) => (
                <MenuItem
                  key={state}
                  value={state}
                  data-cy="export-state-selector-item"
                >
                  <Checkbox checked={field.value.includes(state)} />
                  <ListItemText
                    primary={t(translationStates[state].translationKey)}
                  />
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
