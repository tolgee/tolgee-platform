import { InputLabel, makeStyles } from '@material-ui/core';

const useStyles = makeStyles((theme) => ({
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
