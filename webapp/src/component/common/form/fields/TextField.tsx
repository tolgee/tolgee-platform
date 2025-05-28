import { FunctionComponent, useEffect, useState } from 'react';
import { TextField as TolgeeTextField } from 'tg.component/common/TextField';
import { FieldValidator, useField } from 'formik';

interface PGTextFieldProps {
  name: string;
  onValueChange?: (newValue: string) => void;
  validate?: FieldValidator;
}

type Props = PGTextFieldProps & React.ComponentProps<typeof TolgeeTextField>;

export const TextField: FunctionComponent<Props> = (props) => {
  const [field, meta] = useField(props.name);
  const [oldValue, setOldValue] = useState({
    name: field.value,
    validate: props.validate,
  });

  const { onValueChange: _, ...otherProps } = props;

  useEffect(() => {
    if (typeof props.onValueChange === 'function' && oldValue !== field.value) {
      props.onValueChange(field.value);
      setOldValue(field.value);
    }
  });

  return (
    <TolgeeTextField
      fullWidth={props.fullWidth ?? true}
      {...field}
      {...otherProps}
      helperText={(meta.touched && meta.error) || props.helperText}
      error={!!meta.error && meta.touched}
    />
  );
};
