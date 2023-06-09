import { FunctionComponent, useEffect, useState } from 'react';
import { TextField as MUITextField, TextFieldProps } from '@mui/material';
import { useField } from 'formik';

interface PGTextFieldProps {
  name: string;
  onValueChange?: (newValue: string) => void;
}

type Props = PGTextFieldProps & TextFieldProps;

export const TextField: FunctionComponent<Props> = (props) => {
  const [field, meta] = useField(props.name);
  const [oldValue, setOldValue] = useState(field.value);

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { onValueChange, ...otherProps } = props;

  useEffect(() => {
    if (typeof props.onValueChange === 'function' && oldValue !== field.value) {
      props.onValueChange(field.value);
      setOldValue(field.value);
    }
  });

  return (
    <MUITextField
      sx={{ mt: 2, minHeight: otherProps.size === 'small' ? 50 : 70 }}
      className={props.className}
      fullWidth={props.fullWidth ? props.fullWidth : true}
      {...field}
      {...otherProps}
      helperText={(meta.touched && meta.error) || props.helperText}
      error={!!meta.error && meta.touched}
    />
  );
};
