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
  message: string;
};

export const TabMessage: React.FC<React.PropsWithChildren<Props>> = ({
  type,
  message,
}) => {
  return <StyledWrapper className={type}>{message}</StyledWrapper>;
};
