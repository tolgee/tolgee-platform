import { styled } from '@mui/material';

const StyledWrapper = styled('div')`
  padding: ${({ theme }) => theme.spacing(1, 1.25)};
  color: ${({ theme }) => theme.palette.text.disabled};
`;

type Props = {
  children: React.ReactNode;
};

export const TabMessage: React.FC<Props> = ({ children }) => {
  return <StyledWrapper>{children}</StyledWrapper>;
};
