import { styled } from '@mui/material';
import { PlanInfo } from './PlanInfo';

const StyledItem = styled('div')`
  display: grid;
  justify-items: center;
  color: ${({ theme }) => theme.palette.emphasis[700]};
`;

const StyledSpacer = styled('div')`
  width: 1px;
  background: ${({ theme }) => theme.palette.divider};
`;

const StyledNumber = styled('div')`
  font-size: 24px;
`;

const StyledName = styled('div')`
  font-size: 14px;
  text-align: center;
`;

type Props = {
  left: {
    number: React.ReactNode;
    name: React.ReactNode;
  };
  right: {
    number: React.ReactNode;
    name: React.ReactNode;
  };
};

export const PlanMetrics = ({ left, right }: Props) => {
  return (
    <PlanInfo>
      <StyledItem>
        <StyledNumber>{left.number}</StyledNumber>
        <StyledName>{left.name}</StyledName>
      </StyledItem>
      <StyledSpacer />
      <StyledItem>
        <StyledNumber>{right.number}</StyledNumber>
        <StyledName>{right.name}</StyledName>
      </StyledItem>
    </PlanInfo>
  );
};
