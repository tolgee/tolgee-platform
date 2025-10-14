import { ShieldTick, ShieldOff } from '@untitled-ui/icons-react';
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

const StyledShieldOff = styled(ShieldOff)`
  width: 14px;
  height: 14px;
  color: ${({ theme }) => theme.palette.error.main};
`;

type Props = {
  enabled: boolean;
};

export const MfaBadge = ({ enabled }: Props) => {
  const { t } = useTranslate();

  return (
    <Tooltip
      title={
        enabled ? t('tooltip_user_mfa_enabled') : t('tooltip_user_mfa_disabled')
      }
    >
      <StyledTextWrapper>
        {enabled ? (
          <StyledShieldTick aria-hidden="true" focusable="false" />
        ) : (
          <StyledShieldOff aria-hidden="true" focusable="false" />
        )}
        <StyledText>{t('user_mfa')}</StyledText>
      </StyledTextWrapper>
    </Tooltip>
  );
};
