import React, { ComponentProps, FC } from 'react';
import { useField } from 'formik';
import { useFieldError } from './useFieldError';
import { FormControl, FormHelperText } from '@mui/material';
import { DateTimePicker } from '@mui/x-date-pickers';
import { PropsOf } from '@emotion/react';

type DateTimePickerFieldProps = PropsOf<typeof DateTimePicker>;

type FormControlProps = ComponentProps<typeof FormControl> & {
  'data-cy'?: string;
};
type DateTimePickerProps = PropsOf<typeof DateTimePicker>;

export const DateTimePickerField: FC<
  {
    name: string;
    className?: string;
    formControlProps?: FormControlProps;
    dateTimePickerProps: DateTimePickerProps;
  } & DateTimePickerFieldProps
> = ({ name, formControlProps, dateTimePickerProps }) => {
  const [field, _, helpers] = useField(name);
  const { error, helperText } = useFieldError({ fieldName: name });
  return (
    <FormControl {...formControlProps} error={error}>
      <DateTimePicker
        {...dateTimePickerProps}
        onChange={(value) => helpers.setValue(value)}
        value={field.value}
      />
      {error && <FormHelperText error={error}>{helperText}</FormHelperText>}
    </FormControl>
  );
};
