import { default as React, FunctionComponent, ReactNode } from 'react';
import {
  FormControl,
  FormControlProps,
  FormHelperText,
  InputLabel,
  Select as MUISelect,
  styled,
} from '@mui/material';
import { useField } from 'formik';

interface PGSelectProps {
  name: string;
  label?: ReactNode;
  renderValue?: (v: any) => ReactNode;
}

type Props = PGSelectProps & FormControlProps;

const StyledFormControl = styled(FormControl)`
  margin-top: ${({ theme }) => theme.spacing(2)};
  margin-bottom: ${({ theme }) => theme.spacing(2)};
  min-width: 120px;
`;

export const Select: FunctionComponent<Props> = (props) => {
  const [field, meta, helpers] = useField(props.name);

  const { renderValue, ...formControlProps } = props;

  return (
    <StyledFormControl
      variant={props.variant}
      error={!!meta.error}
      {...formControlProps}
    >
      {props.label && (
        <InputLabel
          variant={props.variant}
          id={'select_' + field.name + '_label'}
        >
          {props.label}
        </InputLabel>
      )}
      <MUISelect
        data-cy="global-form-select"
        name={field.name}
        labelId={'select_' + field.name + '_label'}
        label={props.label}
        value={field.value}
        onChange={(e) => helpers.setValue(e.target.value)}
        renderValue={
          typeof renderValue === 'function'
            ? renderValue
            : (value) => value as ReactNode
        }
      >
        {props.children}
      </MUISelect>
      {meta.error && <FormHelperText>{meta.error}</FormHelperText>}
    </StyledFormControl>
  );
};
