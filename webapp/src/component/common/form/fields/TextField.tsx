import { FunctionComponent, useEffect, useState } from 'react';
import {
  TextField as MUITextField,
  TextFieldProps,
  Theme,
} from '@material-ui/core';
import { createStyles, makeStyles } from '@material-ui/core/styles';
import { useField } from 'formik';

interface PGTextFieldProps {
  name: string;
  onValueChange?: (newValue: string) => void;
}

type Props = PGTextFieldProps & TextFieldProps;

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    textField: {
      marginTop: theme.spacing(2),
      minHeight: 70,
    },
  })
);

export const TextField: FunctionComponent<Props> = (props) => {
  const classes = useStyles({});
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
      className={props.className || classes.textField}
      fullWidth={props.fullWidth ? props.fullWidth : true}
      {...field}
      {...otherProps}
      helperText={meta.touched && meta.error}
      error={!!meta.error && meta.touched}
    />
  );
};
