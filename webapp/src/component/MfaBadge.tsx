import { ShieldTick } from '@untitled-ui/icons-react';
import { styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

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

export const MfaBadge = () => {
  const { t } = useTranslate();

  return (
    <Tooltip title={t('tooltip_user_mfa_enabled')}>
      <StyledTextWrapper>
        <StyledShieldTick aria-hidden="true" focusable="false" />
        <StyledText>2FA</StyledText>
      </StyledTextWrapper>
    </Tooltip>
  );
};
