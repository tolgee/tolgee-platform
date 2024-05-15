import { FunctionComponent } from 'react';
import {
  InputLabel,
  TextField as MUITextField,
  TextFieldProps,
  styled,
  textFieldClasses,
} from '@mui/material';

const StyledContainer = styled('div')`
  display: grid;
  .${textFieldClasses.root} {
    margin-top: 4px;
    min-height: 64px;
  }
`;

const StyledInputLabel = styled(InputLabel)`
  font-size: 14px;
  font-weight: 500px;
`;

type Props = TextFieldProps;

export const TextField: FunctionComponent<Props> = (props) => {
  const { label, ...otherProps } = props;
  return (
    <StyledContainer>
      {label && <StyledInputLabel>{label}</StyledInputLabel>}
      <MUITextField variant="outlined" size="small" {...otherProps} />
    </StyledContainer>
  );
};
