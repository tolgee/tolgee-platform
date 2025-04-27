import { T } from '@tolgee/react';
import { DateTimePickerField } from 'tg.component/common/form/fields/DateTimePickerField';
import React from 'react';

export const AssignTrialDatePickerField = () => {
  return (
    <DateTimePickerField
      formControlProps={{
        'data-cy': 'administration-trial-end-date-field',
        sx: { mt: 1 },
      }}
      dateTimePickerProps={{
        disablePast: true,
        label: (
          <T keyName="administration-subscription-assign-trial-end-date-field" />
        ),
      }}
      name="trialEnd"
    />
  );
};
