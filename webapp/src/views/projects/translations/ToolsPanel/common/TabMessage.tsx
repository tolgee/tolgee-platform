import { styled } from '@mui/material';

const StyledWrapper = styled('div')`
  margin: ${({ theme }) => theme.spacing(1, 1.25)};
  color: ${({ theme }) => theme.palette.text.disabled};
  font-style: italic;
`;

type Props = {
  children: React.ReactNode;
};

export const TabMessage: React.FC<Props> = ({ children }) => {
  return (
    <StyledWrapper onMouseDown={(e) => e.preventDefault()}>
      {children}
    </StyledWrapper>
  );
};
