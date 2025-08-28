import { ShieldTick } from '@untitled-ui/icons-react';
import { styled } from '@mui/material';

const StyledTextWrapper = styled('span')`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
`;

const StyledText = styled('span')`
  font-size: 12px;
  font-weight: 500;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledShieldTick = styled(ShieldTick)`
  width: 14px;
  height: 14px;
  color: ${({ theme }) => theme.palette.success.main};
`;

export const MfaBadge = () => (
  <StyledTextWrapper>
    <StyledShieldTick />
    <StyledText>2FA</StyledText>
  </StyledTextWrapper>
);
