import { styled } from '@mui/material';

const StyledWrapper = styled('div')`
  padding: ${({ theme }) => theme.spacing(1, 1.25)};

  &.placeholder {
    color: ${({ theme }) => theme.palette.text.disabled};
  }

  &.error {
    color: ${({ theme }) => theme.palette.error.dark};
  }
`;

type Props = {
  type: 'placeholder' | 'error';
  message: React.ReactNode;
};

export const TabMessage: React.FC<Props> = ({ type, message }) => {
  return <StyledWrapper className={type}>{message}</StyledWrapper>;
};
