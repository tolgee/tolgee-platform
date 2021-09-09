import { Typography } from '@material-ui/core';

export const FieldLabel: React.FC = ({ children }) => {
  return (
    <Typography data-cy="translation-field-label" variant="subtitle2">
      {children}
    </Typography>
  );
};
