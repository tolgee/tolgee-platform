import { InputLabel, Theme } from '@mui/material';

import makeStyles from '@mui/styles/makeStyles';

const useStyles = makeStyles<Theme>((theme) => ({
  label: {
    fontSize: 14,
    marginBottom: 5,
    fontWeight: 500,
  },
}));

export const FieldLabel: React.FC = ({ children }) => {
  const classes = useStyles();
  return (
    <InputLabel className={classes.label} data-cy="translation-field-label">
      {children}
    </InputLabel>
  );
};
