import { default as React, FunctionComponent } from 'react';
import {
  Checkbox,
  FormControl,
  FormControlLabel,
  FormControlProps,
  FormHelperText,
  Theme,
} from '@mui/material';
import createStyles from '@mui/styles/createStyles';
import makeStyles from '@mui/styles/makeStyles';
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
  const useStyles = makeStyles<Theme>((theme) =>
    createStyles({
      checkbox: {
        marginTop: theme.spacing(props.mt !== undefined ? props.mt : 2),
        marginBottom: theme.spacing(props.mb !== undefined ? props.mb : 2),
      },
    })
  );

  const classes = useStyles({});

  const [field, meta] = useField(props.name);

  return (
    <FormControl className={classes.checkbox} error={!!meta.error} {...props}>
      <FormControlLabel
        control={<Checkbox {...field} />}
        label={props.label ?? ''}
      />
      {meta.error && <FormHelperText>{meta.error}</FormHelperText>}
    </FormControl>
  );
};
