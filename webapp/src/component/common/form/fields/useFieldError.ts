import { useField } from 'formik';
import { ReactNode } from 'react';

export const useFieldError = ({
  fieldName,
  customHelperText,
}: {
  fieldName: string;
  customHelperText?: ReactNode;
}) => {
  const [_, meta] = useField(fieldName);

  return {
    helperText: (meta.touched && meta.error) || customHelperText,
    error: Boolean(meta.touched && meta.error),
    errorTextWhenTouched: (meta.touched && meta.error) || undefined,
  };
};
