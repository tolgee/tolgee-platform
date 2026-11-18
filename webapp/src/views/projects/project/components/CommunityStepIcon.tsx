import clsx from 'clsx';
import { Box, StepIconProps, styled } from '@mui/material';
import { Check } from '@untitled-ui/icons-react';

export const StyledStepIndex = styled(Box)`
  display: flex;
  flex: none;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 13px;
  background: ${({ theme }) => theme.palette.quickStart.circleNormal};
  &.active {
    font-weight: 600;
  }
  &.done {
    background: ${({ theme }) => theme.palette.quickStart.circleSuccess};
    color: ${({ theme }) => theme.palette.emphasis[50]};
  }
`;

export const CommunityStepIcon = ({
  icon,
  active,
  completed,
}: StepIconProps) => (
  <StyledStepIndex className={clsx({ active, done: completed })}>
    {completed ? <Check width={16} height={16} /> : icon}
  </StyledStepIndex>
);
