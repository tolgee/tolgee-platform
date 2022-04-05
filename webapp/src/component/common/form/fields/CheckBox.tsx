import { FunctionComponent } from 'react';
import {
  Checkbox,
  FormControl,
  FormControlLabel,
  FormControlProps,
  FormHelperText,
} from '@mui/material';
import { useField } from 'formik';

interface PGCheckBoxProps {
  name: string;
  label?: string;
  color?: 'primary' | 'secondary' | 'default';
  mt?: number;
  mb?: number;
}

type Props = PGCheckBoxProps & FormControlProps;

export const CheckBox: FunctionComponent<Props> = (props) => {
  const [field, meta] = useField(props.name);

  return (
    <FormControl
      sx={{
        mt: props.mt !== undefined ? props.mt : 2,
        mb: props.mb !== undefined ? props.mb : 2,
      }}
      error={!!meta.error}
      {...props}
    >
      <FormControlLabel control={<Checkbox {...field} />} label={props.label} />
      {meta.error && <FormHelperText>{meta.error}</FormHelperText>}
    </FormControl>
  );
};
