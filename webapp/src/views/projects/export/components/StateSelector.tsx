import { Field } from 'formik';
import { Checkbox, ListItemText, MenuItem } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { Select } from 'tg.component/common/Select';
import { useStateTranslation } from 'tg.translationTools/useStateTranslation';
import { StateType, EXPORTABLE_STATES } from 'tg.constants/translationStates';

type Props = {
  className?: string;
};

export const StateSelector: React.FC<React.PropsWithChildren<Props>> = ({
  className,
}) => {
  const { t } = useTranslate();
  const translateState = useStateTranslation();

  return (
    <Field name="states">
      {({ field, meta }) => {
        return (
          <div className={className}>
            <Select
              {...field}
              label={t('export_translations_states_label')}
              error={meta.error}
              minHeight={false}
              shrinkable
              data-cy="export-state-selector"
              renderValue={(values: StateType[]) =>
                values.map((val) => translateState(val)).join(', ')
              }
              multiple
            >
              {EXPORTABLE_STATES.map((state) => (
                <MenuItem
                  key={state}
                  value={state}
                  data-cy="export-state-selector-item"
                  data-cy-state={state}
                >
                  <Checkbox checked={field.value.includes(state)} />
                  <ListItemText primary={translateState(state as StateType)} />
                </MenuItem>
              ))}
            </Select>
          </div>
        );
      }}
    </Field>
  );
};
