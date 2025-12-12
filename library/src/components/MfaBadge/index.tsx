import { ShieldTick, ShieldOff } from '@untitled-ui/icons-react';
import { Tooltip } from '@mui/material';
import { styled } from '@mui/material/styles';
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

/**
 * MfaBadge is a static element that is used to display user's Multi Factor Authentication (MFA) state. It communicates whether MFA is enabled or disabled. The state is reflected by the component's icon and by its tooltip.
 */
export const MfaBadge = ({ enabled }: Props) => {
  const { t } = useTranslate();

  return (
    <Tooltip
      title={
        enabled ? t('tooltip_user_mfa_enabled') : t('tooltip_user_mfa_disabled')
      }
      data-cy="mfa-badge"
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
