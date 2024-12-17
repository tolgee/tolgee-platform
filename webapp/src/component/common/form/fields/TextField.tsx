import { FunctionComponent, useEffect, useState } from 'react';
import { TextField as TolgeeTextField } from 'tg.component/common/TextField';
import { useField } from 'formik';

interface PGTextFieldProps {
  name: string;
  onValueChange?: (newValue: string) => void;
}

type Props = PGTextFieldProps & React.ComponentProps<typeof TolgeeTextField>;

export const TextField: FunctionComponent<Props> = (props) => {
  const [field, meta] = useField(props.name);
  const [oldValue, setOldValue] = useState(field.value);

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
