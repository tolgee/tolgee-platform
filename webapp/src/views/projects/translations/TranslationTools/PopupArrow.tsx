import { styled } from '@mui/material';

const SIZE = 12;
const PADDING = 10;

const StyledWrapper = styled('div')`
  top: ${-(SIZE + PADDING)}px;
  padding: ${PADDING}px;
  padding-bottom: 0px;
  position: absolute;
  overflow-y: hidden;
  pointer-events: none;
`;

const StyledArrow = styled('div')`
  width: 0px;
  height: 0px;
  border-left: ${SIZE}px solid transparent;
  border-right: ${SIZE}px solid transparent;
  border-bottom: ${SIZE}px solid
    ${({ theme }) => theme.palette.cellSelected1.main};
  filter: drop-shadow(0px 3px 5px rgba(0, 0, 0, 0.5));
`;

type Props = {
  position: string;
};

export const PopupArrow: React.FC<Props> = ({ position }) => {
  return (
    <StyledWrapper style={{ left: `calc(${position} - ${PADDING + SIZE}px)` }}>
      <StyledArrow />
    </StyledWrapper>
  );
};
