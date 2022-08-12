import { FunctionComponent } from 'react';
import {
  Checkbox,
  FormControl,
  FormControlLabel,
  FormControlProps,
  FormGroup,
  FormHelperText,
  FormLabel,
} from '@mui/material';
import { useField } from 'formik';

interface CheckBoxGroupMultiSelectProps {
  name: string;
  label?: string;
  color?: 'primary' | 'secondary' | 'default';
  options: Set<string>;
}

type Props = CheckBoxGroupMultiSelectProps & FormControlProps;

export const CheckBoxGroupMultiSelect: FunctionComponent<Props> = (props) => {
  const [field, meta, helpers] = useField<Set<string>>(props.name);

  const onChange = (option, checked) => {
    const newValue = new Set(field.value);
    newValue.add(option);
    if (!checked) {
      newValue.delete(option);
    }
    helpers.setValue(newValue);
  };

  return (
    <FormGroup>
      <FormLabel error={!!meta.error} component="legend">
        {props.label}
      </FormLabel>
      {Array.from(props.options).map((option, index) => {
        return (
          <FormControl
            key={index}
            error={!!meta.error}
            sx={{
              mt: 0.5,
              mb: 0.5,
            }}
          >
            <FormControlLabel
              label={option}
              control={
                <Checkbox
                  onChange={(e) => onChange(option, e.target.checked)}
                  checked={field.value.has(option)}
                />
              }
            />
          </FormControl>
        );
      })}
      {!!meta.error && (
        <FormHelperText error={!!meta.error}>{meta.error}</FormHelperText>
      )}
    </FormGroup>
  );
};
