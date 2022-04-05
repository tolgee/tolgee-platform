import { InputLabel, styled } from '@mui/material';

const StyledInputLabel = styled(InputLabel)`
  font-size: 14px;
  margin-bottom: 5px;
  font-weight: 500px;
`;

export const FieldLabel: React.FC = ({ children }) => {
  return (
    <StyledInputLabel data-cy="translation-field-label">
      {children}
    </StyledInputLabel>
  );
};
