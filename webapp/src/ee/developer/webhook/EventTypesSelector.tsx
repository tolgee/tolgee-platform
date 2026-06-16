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

const EVENT_TYPES = ['PROJECT_ACTIVITY', 'CONTENT_DELIVERY_PUBLISH'] as const;

export const EventTypesSelector = () => {
  const { t } = useTranslate();
  return (
    <Field name="eventTypes">
      {({ field, meta }) => (
        <FormControl fullWidth error={Boolean(meta.error)} variant="standard">
          <InputLabel>
            {t('webhook_form_event_types_label', 'Event types')}
          </InputLabel>
          <Select
            {...field}
            multiple
            variant="standard"
            data-cy="webhook-event-types-selector"
            renderValue={(values: string[]) =>
              values
                .map((v) =>
                  t(
                    `webhook_event_type_${v.toLowerCase()}`,
                    v.toLowerCase().replace(/_/g, ' ')
                  )
                )
                .join(', ')
            }
          >
            {EVENT_TYPES.map((type) => (
              <MenuItem
                key={type}
                value={type}
                data-cy={`webhook-event-type-${type.toLowerCase()}`}
              >
                <Checkbox checked={field.value.includes(type)} />
                <ListItemText
                  primary={t(
                    `webhook_event_type_${type.toLowerCase()}`,
                    type.toLowerCase().replace(/_/g, ' ')
                  )}
                />
              </MenuItem>
            ))}
          </Select>
          <FormHelperText>{meta.error}</FormHelperText>
        </FormControl>
      )}
    </Field>
  );
};
