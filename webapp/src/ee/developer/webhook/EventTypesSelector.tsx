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

type EventType = (typeof EVENT_TYPES)[number];

function eventTypeLabel(
  type: EventType | string,
  t: ReturnType<typeof useTranslate>['t']
): string {
  switch (type) {
    case 'PROJECT_ACTIVITY':
      return t('webhook_event_type_project_activity', 'Project activity');
    case 'CONTENT_DELIVERY_PUBLISH':
      return t(
        'webhook_event_type_content_delivery_publish',
        'Content delivery publish'
      );
    default:
      return type;
  }
}

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
              values.map((v) => eventTypeLabel(v, t)).join(', ')
            }
          >
            {EVENT_TYPES.map((type) => (
              <MenuItem key={type} value={type}>
                <Checkbox checked={field.value.includes(type)} />
                <ListItemText primary={eventTypeLabel(type, t)} />
              </MenuItem>
            ))}
          </Select>
          <FormHelperText>{meta.error}</FormHelperText>
        </FormControl>
      )}
    </Field>
  );
};
