import { styled } from '@mui/material';

const StyledWrapper = styled('div')`
  height: 100%;
  padding: 8px 12px 8px 12px;
  margin: 0px 8px;
  display: flex;
  flex-direction: column;
  position: relative;
  background: ${({ theme }) => theme.palette.cell.selected};
  border-radius: 8px;
`;

type Props = {
  children: React.ReactNode;
};

export const TabMessage: React.FC<Props> = ({ children }) => {
  return <StyledWrapper>{children}</StyledWrapper>;
};
