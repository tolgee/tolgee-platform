import { styled } from '@mui/material';

const StyledContainer = styled('span')`
  display: inline-flex;
  align-items: center;
  background: linear-gradient(
    -225deg,
    rgb(213, 219, 228) 0%,
    rgb(248, 248, 248) 100%
  );
  border-radius: 3px;
  box-shadow: inset 0 -2px 0 0 rgb(205, 205, 230), inset 0 0 1px 1px #fff,
    0 1px 2px 1px rgba(30, 35, 90, 0.4);
  color: rgb(127, 132, 151);
  height: 18px;
  justify-content: center;
  margin: 0px 4px;
  padding-bottom: 2px;
  position: relative;
  top: -1px;
  width: 20px;
  vertical-align: bottom;
  font-size: ${({ theme }) => theme.typography.caption.fontSize}px;
`;

export const KeyTemplate: React.FC = ({ children }) => {
  return <StyledContainer>{children}</StyledContainer>;
};
