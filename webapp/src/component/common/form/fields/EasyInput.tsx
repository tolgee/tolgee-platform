import { default as React, FunctionComponent } from 'react';
import { FormHelperText, Input, InputProps } from '@mui/material';
import { useField } from 'formik';

interface EasyInputProps {
  name: string;
}

type Props = EasyInputProps & InputProps;

export const EasyInput: FunctionComponent<Props> = (props) => {
  const [field, meta] = useField(props.name);

  const onChange = (e) => {
    field.onChange(e);
    if (props.onChange) {
      props.onChange(e);
    }
  };

  return (
    <>
      <Input
        {...field}
        {...props}
        onChange={onChange}
        error={!!meta.error}
        inputRef={props.inputRef}
      />
      <FormHelperText error>{meta.error}</FormHelperText>
    </>
  );
};
