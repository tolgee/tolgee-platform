import { FunctionComponent } from 'react';
import {
  InputLabel,
  TextField as MUITextField,
  styled,
  textFieldClasses,
} from '@mui/material';

const StyledContainer = styled('div')`
  display: grid;
  .${textFieldClasses.root} {
    margin-top: 4px;
  }
`;

const StyledInputLabel = styled(InputLabel)`
  font-size: 14px;
  font-weight: 500px;
`;

export type TextFieldProps = React.ComponentProps<typeof MUITextField> & {
  minHeight?: boolean;
};

export const TextField: FunctionComponent<TextFieldProps> = (props) => {
  const { label, minHeight = true, sx, ...otherProps } = props;
  return (
    <StyledContainer>
      {label && <StyledInputLabel>{label}</StyledInputLabel>}
      <MUITextField
        variant="outlined"
        size="small"
        sx={{ minHeight: minHeight ? '64px' : undefined, ...sx }}
        {...otherProps}
      />
    </StyledContainer>
  );
};
